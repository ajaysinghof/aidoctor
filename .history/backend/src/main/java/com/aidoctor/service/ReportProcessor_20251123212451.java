package com.aidoctor.service;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.model.TestResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ReportProcessor: fixes broken OCR, classifies, extracts tests, and asks AI for interpretation.
 * Keeps original heuristics but makes them consistent with model types.
 */
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

    // crude "broken OCR" detector
    private boolean isOcrBroken(String text) {
        if (text == null) return true;
        String t = text.trim();
        if (t.length() < 40) return true; // too small
        int nonAlphaDigits = 0;
        for (char c : t.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && ",./:%()[]-".indexOf(c) < 0) nonAlphaDigits++;
        }
        if (((double) nonAlphaDigits / Math.max(1, t.length())) > 0.12) return true;
        return false;
    }

    public MedicalReport process(Map<String, String> fields, String ocrText) {

        MedicalReport mr = new MedicalReport();
        String cleanedText = ocrText == null ? "" : ocrText;

        // 1) If OCR seems broken, ask AI to fix
        if (isOcrBroken(cleanedText)) {
            String corrected = aiService.fixBrokenMedicalText(cleanedText);
            if (corrected != null && !corrected.isBlank()) cleanedText = corrected;
        }

        // 2) classify using keywords
        DocumentClassifier.DocType type = classifier.classifyByKeywords(cleanedText);

        // 3) fallback to AI classification if unknown
        if (type == DocumentClassifier.DocType.UNKNOWN) {
            type = aiService.classifyDocumentWithAi(cleanedText);
        }

        // 4) If not a medical document (explicit NON_MEDICAL or UNKNOWN) -> return friendly error
        if (type == DocumentClassifier.DocType.NON_MEDICAL || type == DocumentClassifier.DocType.UNKNOWN) {
            mr.setSummary("❌ This does not appear to be a medical document. Please upload lab reports (CBC, LFT, KFT, Lipid Profile) or a prescription.");
            mr.setTests(new HashMap<>());
            mr.setDocType(type.name());
            mr.setText(cleanedText);
            return mr;
        }

        // 5) Build structured tests map from fields (your OCR fields map)
        Map<String, TestResult> testsMap = new HashMap<>();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            String name = e.getKey().trim();
            String raw = e.getValue().trim();
            TestResult tr = new TestResult();
            tr.setName(name);
            tr.setRawValue(raw);
            // basic numeric extraction - crude
            try {
                String numOnly = raw.replaceAll("[^0-9.\\-]", "");
                if (!numOnly.isBlank()) tr.setValue(Double.parseDouble(numOnly));
            } catch (Exception ignored) {}
            testsMap.put(name, tr);
        }

        // 6) If OCR fields empty, attempt to extract key-value pairs heuristically from cleanedText
        if (testsMap.isEmpty()) {
            String[] lines = cleanedText.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].trim();
                    String val = parts[1].trim();
                    TestResult tr = new TestResult();
                    tr.setName(key);
                    tr.setRawValue(val);
                    try {
                        String numOnly = val.replaceAll("[^0-9.\\-]", "");
                        if (!numOnly.isBlank()) tr.setValue(Double.parseDouble(numOnly));
                    } catch (Exception ignored) {}
                    testsMap.put(key, tr);
                }
            }
        }

        // 7) Ask AI for interpretation and summary (pass cleanedText and structured tests)
        Map<String, String> simpleTests = new HashMap<>();
        for (Map.Entry<String, TestResult> me : testsMap.entrySet()) {
            simpleTests.put(me.getKey(), me.getValue().getRawValue());
        }

        String aiJson = aiService.summarizeAndInterpret(cleanedText, simpleTests);

        // Fill MedicalReport
        mr.setText(cleanedText);
        mr.setDocType(type.name());
        mr.setAiInterpretation(aiJson);
        mr.setSummary(extractSummaryFromAiReply(aiJson));
        mr.setTests(testsMap);

        // enhance each TestResult with an interpretation from AI
        for (Map.Entry<String, TestResult> me : testsMap.entrySet()) {
            TestResult tr = me.getValue();
            // if numeric -> numeric interpreter
            if (tr.getValue() != null) {
                String interp = aiService.interpretNumericTest(tr.getName(), tr.getValue(), tr.getUnit());
                tr.setInterpretation(interp);
            } else {
                String interp = aiService.interpretRawTest(tr.getName(), tr.getRawValue());
                tr.setInterpretation(interp);
            }
        }

        // 8) Also generate a clean PDF and attach raw bytes (or store S3 / DB)
        byte[] pdf = pdfService.generateReportPdf("AI Doctor - Corrected Report", cleanedText, simpleTests, mr.getAiInterpretation());
        mr.setPdfBytes(pdf);

        return mr;
    }

    // crude parser to extract "summary" field from aiJson (which may be JSON or text)
    private String extractSummaryFromAiReply(String aiReply) {
        if (aiReply == null) return "";
        int idx = aiReply.toLowerCase().indexOf("\"summary\"");
        if (idx >= 0) {
            int colon = aiReply.indexOf(":", idx);
            if (colon > 0) {
                int start = aiReply.indexOf("\"", colon);
                if (start > 0) {
                    int end = aiReply.indexOf("\"", start + 1);
                    if (end > start) return aiReply.substring(start + 1, end);
                }
            }
        }
        return aiReply.length() > 300 ? aiReply.substring(0, 300) + "..." : aiReply;
    }
}
