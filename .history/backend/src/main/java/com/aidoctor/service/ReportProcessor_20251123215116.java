package com.aidoctor.service;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.model.TestResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportProcessor {

    private final DocumentClassifier classifier;
    private final AiService aiService;
    private final PdfService pdfService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportProcessor(DocumentClassifier classifier, AiService aiService, PdfService pdfService) {
        this.classifier = classifier;
        this.aiService = aiService;
        this.pdfService = pdfService;
    }

    // --- OCR Quality Detection ---
    private boolean isOcrBroken(String text) {
        if (text == null) return true;
        String t = text.trim();
        if (t.length() < 40) return true; // too short to be medical

        int nonAlpha = 0;
        for (char c : t.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)
                    && ",./:%()[]-".indexOf(c) < 0)
                nonAlpha++;
        }
        return ((double) nonAlpha / Math.max(1, t.length())) > 0.12;
    }

    // --- MAIN METHOD ---
    public MedicalReport process(Map<String, String> fields, String ocrText) {
        MedicalReport mr = new MedicalReport();
        String cleaned = ocrText == null ? "" : ocrText;

        // STEP 1: Fix broken OCR with AI
        if (isOcrBroken(cleaned)) {
            String fixed = aiService.fixBrokenMedicalText(cleaned);
            if (fixed != null && !fixed.isBlank()) cleaned = fixed;
        }

        // STEP 2: Keyword-based doc classification
        DocumentClassifier.DocType type = classifier.classifyByKeywords(cleaned);

        // STEP 3: AI fallback for classification
        if (type == DocumentClassifier.DocType.UNKNOWN) {
            type = aiService.classifyDocumentWithAi(cleaned);
        }

        // STEP 4: Validate MEDICAL document
        if (type == DocumentClassifier.DocType.NON_MEDICAL || type == DocumentClassifier.DocType.UNKNOWN) {
            mr.setDocType(type.name());
            mr.setSummary("❌ This does not appear to be a medical report. Please upload CBC, LFT, KFT, Lipid Profile or similar.");
            mr.setTests(new HashMap<>());
            return mr;
        }

        // STEP 5: Parse tests from OCR fields
        Map<String, TestResult> structuredTests = new HashMap<>();
        for (var e : fields.entrySet()) {
            TestResult tr = new TestResult();
            tr.setName(e.getKey());
            tr.setRawValue(e.getValue());

            try {
                String num = e.getValue().replaceAll("[^0-9.\\-]", "");
                if (!num.isBlank()) tr.setValue(Double.parseDouble(num));
            } catch (Exception ignored) {}

            structuredTests.put(e.getKey(), tr);
        }

        // STEP 6: If no structured fields, extract from raw text
        if (structuredTests.isEmpty()) {
            String[] lines = cleaned.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].trim();
                    String val = parts[1].trim();
                    TestResult tr = new TestResult();
                    tr.setName(key);
                    tr.setRawValue(val);

                    try {
                        String num = val.replaceAll("[^0-9.\\-]", "");
                        if (!num.isBlank()) tr.setValue(Double.parseDouble(num));
                    } catch (Exception ignored) {}

                    structuredTests.put(key, tr);
                }
            }
        }

        // STEP 7: Prepare simple map for AI
        Map<String, String> simpleMap = new HashMap<>();
        structuredTests.forEach((k, v) -> simpleMap.put(k, v.getRawValue()));

        // STEP 8: AI Summary + Interpretation (forced JSON)
        String aiResponse = aiService.summarizeAndInterpret(cleaned, simpleMap);

        // STEP 9: Extract high-quality summary
        String summary = extractSummary(aiResponse);

        // STEP 10: Fill MedicalReport
        mr.setText(cleaned);
        mr.setDocType(type.name());
        mr.setSummary(summary);
        mr.setAiInterpretation(aiResponse);
        mr.setTests(structuredTests);

        // STEP 11: Generate PDF
        try {
            byte[] pdf = pdfService.generateReportPdf(
                    "AI Doctor Analysis Report",
                    cleaned,
                    simpleMap,
                    aiResponse
            );
            mr.setPdfBytes(pdf);
        } catch (Exception ignored) {}

        return mr;
    }

    // --- SUMMARY EXTRACTION (POWERFUL + SAFE + JSON) ---
    private String extractSummary(String aiReply) {
        if (aiReply == null || aiReply.isBlank()) return "No summary available";

        // Try to parse as JSON
        try {
            JsonNode root = mapper.readTree(aiReply);
            if (root.has("summary")) {
                return root.get("summary").asText();
            }
        } catch (Exception ignored) {
            // Not valid JSON
        }

        // Fallback: first non-empty line
        String[] lines = aiReply.split("\\r?\\n");
        for (String line : lines) {
            if (!line.isBlank()) return line.trim();
        }

        // Final fallback
        return aiReply.length() > 200 ? aiReply.substring(0, 200) : aiReply;
    }
}
