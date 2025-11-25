package com.aidoctor.service;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.model.TestResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportProcessor {

    private final DocumentClassifier classifier;
    private final AiService aiService;
    private final PdfService pdfService;

    public ReportProcessor(DocumentClassifier classifier, AiService aiService, PdfService pdfService) {
        this.classifier = classifier;
        this.aiService = aiService;
        this.pdfService = pdfService;
    }

    // ---------------------------
    // BASIC OCR QUALITY CHECK
    // ---------------------------
    private boolean isOcrBroken(String text) {
        if (text == null || text.trim().isEmpty()) return true;

        String t = text.trim();
        if (t.length() < 40) return true; // too short = broken OCR

        int nonAlphaDigits = 0;
        for (char c : t.toCharArray()) {
            if (!Character.isLetterOrDigit(c)
                    && !Character.isWhitespace(c)
                    && ",./:%()[]-".indexOf(c) < 0)
                nonAlphaDigits++;
        }
        double ratio = (double) nonAlphaDigits / Math.max(1, t.length());
        return ratio > 0.12; // if too many weird OCR chars → broken
    }

    // ---------------------------
    // MAIN PROCESSOR
    // ---------------------------
    public MedicalReport process(Map<String, String> fields, String ocrText) {

        MedicalReport mr = new MedicalReport();
        String cleanedText = (ocrText == null) ? "" : ocrText;

        // --- 1) Fix OCR if broken ---
        if (isOcrBroken(cleanedText)) {
            String corrected = aiService.fixBrokenMedicalText(cleanedText);
            if (corrected != null && !corrected.isBlank()) {
                cleanedText = corrected;
            }
        }

        // --- 2) Classify document ---
        DocumentClassifier.DocType type = classifier.classifyByKeywords(cleanedText);

        // --- 3) AI fallback classifier ---
        if (type == DocumentClassifier.DocType.UNKNOWN) {
            type = aiService.classifyDocumentWithAi(cleanedText);
        }

        // --- 4) NOT A MEDICAL DOCUMENT ---
        if (type == DocumentClassifier.DocType.NON_MEDICAL ||
                type == DocumentClassifier.DocType.UNKNOWN) {

            mr.setSummary("❌ This does not appear to be a medical document. "
                    + "Please upload lab reports like CBC, LFT, KFT, Lipid Profile, or a doctor prescription.");
            mr.setTests(new HashMap<>());
            mr.setDocType(type.name());
            return mr;
        }

        // --- 5) Build structured tests map first from OCR fields ---
        Map<String, TestResult> testsMap = new HashMap<>();

        for (Map.Entry<String, String> e : fields.entrySet()) {
            String name = e.getKey().trim();
            String raw = e.getValue().trim();

            TestResult tr = new TestResult();
            tr.setName(name);
            tr.setRawValue(raw);

            try {
                String numOnly = raw.replaceAll("[^0-9.\\-]", "");
                if (!numOnly.isBlank()) {
                    tr.setValue(Double.parseDouble(numOnly));
                }
            } catch (Exception ignore) {}

            testsMap.put(name, tr);
        }

        // --- 6) If OCR fields empty, extract from cleaned text ---
        if (testsMap.isEmpty()) {
            String[] lines = cleanedText.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length < 2) continue;

                    String key = parts[0].trim();
                    String val = parts[1].trim();

                    TestResult tr = new TestResult();
                    tr.setName(key);
                    tr.setRawValue(val);

                    try {
                        String numOnly = val.replaceAll("[^0-9.\\-]", "");
                        if (!numOnly.isBlank()) {
                            tr.setValue(Double.parseDouble(numOnly));
                        }
                    } catch (Exception ignore) {}

                    testsMap.put(key, tr);
                }
            }
        }

        // --- 7) Ask AI for interpretation & summary ---
        Map<String, String> simpleTests = new HashMap<>();
        testsMap.forEach((k, v) -> simpleTests.put(k, v.getRawValue()));

        String aiJson = aiService.summarizeAndInterpret(cleanedText, simpleTests);

        mr.setText(cleanedText);
        mr.setDocType(type.name());
        mr.setTests(simpleTests);
        mr.setAiInterpretation(aiJson);
        mr.setSummary(extractSummaryFromAiReply(aiJson));

        // --- 8) Generate PDF ---
        byte[] pdf = pdfService.generateReportPdf(
                "AI Doctor - Corrected Report",
                cleanedText,
                simpleTests,
                mr.getAiInterpretation()
        );
        mr.setPdfBytes(pdf);

        return mr;
    }

    // ------------------------------------
    // BASIC SUMMARY EXTRACTOR FROM JSON
    // ------------------------------------
    private String extractSummaryFromAiReply(String aiReply) {
        if (aiReply == null) return "";

        String lower = aiReply.toLowerCase();
        int idx = lower.indexOf("\"summary\"");
        if (idx >= 0) {
            int colon = aiReply.indexOf(":", idx);
            if (colon > 0) {
                int start = aiReply.indexOf("\"", colon);
                if (start > 0) {
                    int end = aiReply.indexOf("\"", start + 1);
                    if (end > start) {
                        return aiReply.substring(start + 1, end);
                    }
                }
            }
        }

        // fallback plain summary
        return aiReply.length() > 300 ?
                aiReply.substring(0, 300) + "..." :
                aiReply;
    }
}
