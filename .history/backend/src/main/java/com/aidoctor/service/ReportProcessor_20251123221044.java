package com.aidoctor.service;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.model.TestResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Improved ReportProcessor:
 * - fixes broken OCR with AI
 * - classifies document
 * - extracts tests (from OCR fields or raw text)
 * - asks AI for structured interpretation (JSON)
 * - generates PDF bytes via PdfService
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

    // heuristic for broken OCR
    private boolean isOcrBroken(String text) {
        if (text == null) return true;
        String t = text.trim();
        if (t.length() < 40) return true;
        int nonAlpha = 0;
        for (char c : t.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && ",./:%()[]-±<>".indexOf(c) < 0) nonAlpha++;
        }
        return ((double) nonAlpha / Math.max(1, t.length())) > 0.12;
    }

    private Double tryParseNumber(String raw) {
        if (raw == null) return null;
        String numOnly = raw.replaceAll("[^0-9.\\-]", "");
        if (numOnly.isBlank()) return null;
        try {
            return Double.parseDouble(numOnly);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Main processing entrypoint used by ReportService.
     * Accepts OCR fields (key->value) and the whole OCR text (may be empty).
     */
    public MedicalReport process(Map<String, String> fields, String ocrText) {
        MedicalReport mr = new MedicalReport();

        String cleanedText = ocrText == null ? "" : ocrText;

        // 1. Fix broken OCR using AI if it looks broken
        if (isOcrBroken(cleanedText)) {
            String attempt = aiService.fixBrokenMedicalText(cleanedText);
            if (attempt != null && !attempt.isBlank()) cleanedText = attempt;
        }

        // 2. Classify
        DocumentClassifier.DocType type = classifier.classifyByKeywords(cleanedText);
        if (type == DocumentClassifier.DocType.UNKNOWN) {
            type = aiService.classifyDocumentWithAi(cleanedText);
        }

        if (type == DocumentClassifier.DocType.NON_MEDICAL || type == DocumentClassifier.DocType.UNKNOWN) {
            mr.setSummary("❌ This does not appear to be a medical document. Please upload lab reports (CBC, LFT, KFT, Lipid) or a prescription.");
            mr.setDocType(type.name());
            mr.setTests(Collections.emptyMap());
            mr.setAiInterpretation("");
            mr.setPdfBytes(null);
            return mr;
        }

        // 3. Build structured tests map
        Map<String, TestResult> tests = new LinkedHashMap<>();

        // prefer OCR fields first
        if (fields != null && !fields.isEmpty()) {
            for (Map.Entry<String, String> e : fields.entrySet()) {
                String name = e.getKey().trim();
                String raw = e.getValue().trim();
                TestResult tr = new TestResult();
                tr.setName(name);
                tr.setRawValue(raw);
                Double v = tryParseNumber(raw);
                if (v != null) tr.setValue(v);
                tests.put(name, tr);
            }
        }

        // 4. If none found, fallback to regex extraction from cleanedText (e.g., "Hemoglobin: 12.3 g/dL")
        if (tests.isEmpty() && cleanedText != null && !cleanedText.isBlank()) {
            Pattern p = Pattern.compile("([A-Za-z][A-Za-z \\-\\/\\.]{1,40}?)[:\\t\\s]{1,6}([0-9]+(?:\\.[0-9]+)?(?:\\s*[a-zA-Z/%\\-µμ]*)?)");
            Matcher m = p.matcher(cleanedText);
            while (m.find()) {
                String name = m.group(1).trim();
                String val = m.group(2).trim();
                TestResult tr = new TestResult();
                tr.setName(name);
                tr.setRawValue(val);
                Double vv = tryParseNumber(val);
                if (vv != null) tr.setValue(vv);
                tests.putIfAbsent(name, tr);
            }
        }

        // 5. Ask AI for per-test interpretation (if tests exist) and overall summary + recommendations
        Map<String, String> simpleMap = new LinkedHashMap<>();
        for (Map.Entry<String, TestResult> en : tests.entrySet()) {
            simpleMap.put(en.getKey(), en.getValue().getRawValue());
        }

        String aiReply = aiService.summarizeAndInterpret(cleanedText, simpleMap); // returns JSON-like string

        // 6. If AI couldn't help and we have numeric values, compute very simple heuristics
        if ((aiReply == null || aiReply.isBlank()) && !tests.isEmpty()) {
            StringJoiner sj = new StringJoiner("\n");
            sj.add("{ \"summary\": \"Auto-generated summary\", \"findings\": [], \"recommendations\": [] }");
            aiReply = sj.toString();
        }

        // 7. Populate MedicalReport
        mr.setText(cleanedText);
        mr.setDocType(type.name());
        mr.setTests(tests);
        mr.setAiInterpretation(aiReply);

        // try extract a short summary from aiReply (if it's JSON with a "summary" field)
        String summary = extractSummaryFromAiReply(aiReply);
        mr.setSummary(summary);

        // 8. Generate a PDF bytes to store/display
        byte[] pdf = pdfService.generateReportPdf("AI Doctor - Report", cleanedText, simpleMap, aiReply);
        mr.setPdfBytes(pdf);

        return mr;
    }

    // tries to pull "summary" value from a JSON-like ai reply
    private String extractSummaryFromAiReply(String aiReply) {
        if (aiReply == null) return "";
        String lower = aiReply.toLowerCase();
        int idx = lower.indexOf("\"summary\"");
        if (idx >= 0) {
            int colon = aiReply.indexOf(":", idx);
            if (colon > 0) {
                int firstQuote = aiReply.indexOf("\"", colon);
                if (firstQuote > 0) {
                    int secondQuote = aiReply.indexOf("\"", firstQuote + 1);
                    if (secondQuote > firstQuote) {
                        return aiReply.substring(firstQuote + 1, secondQuote);
                    }
                }
            }
        }
        // fallback: trim and shorten
        String cand = aiReply.trim();
        if (cand.length() > 300) return cand.substring(0, 300) + "...";
        return cand.isBlank() ? "" : cand;
    }
}
