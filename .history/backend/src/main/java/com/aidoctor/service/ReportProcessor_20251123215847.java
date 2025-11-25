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

    // Detect broken OCR
    private boolean isOcrBroken(String text) {
        if (text == null) return true;
        String t = text.trim();
        if (t.length() < 40) return true;

        int noise = 0;
        for (char c : t.toCharArray()) {
            if (!Character.isLetterOrDigit(c) &&
                !Character.isWhitespace(c) &&
                ",./:%()[]-".indexOf(c) < 0) {
                noise++;
            }
        }
        return ((double) noise / Math.max(1, t.length())) > 0.12;
    }

    public MedicalReport process(Map<String, String> fields, String ocrText) {
        MedicalReport mr = new MedicalReport();
        String cleanedText = (ocrText == null) ? "" : ocrText;

        // 1. Fix broken OCR
        if (isOcrBroken(cleanedText)) {
            String corrected = aiService.fixBrokenMedicalText(cleanedText);
            if (corrected != null && !corrected.isBlank()) cleanedText = corrected;
        }

        // 2. classification
        DocumentClassifier.DocType type = classifier.classifyByKeywords(cleanedText);

        if (type == DocumentClassifier.DocType.UNKNOWN) {
            type = aiService.classifyDocumentWithAi(cleanedText);
        }

        // 3. Not medical → return rejection
        if (type == DocumentClassifier.DocType.NON_MEDICAL || type == DocumentClassifier.DocType.UNKNOWN) {
            mr.setDocType(type.name());
            mr.setText(cleanedText);
            mr.setSummary("❌ This is not a medical document. Please upload a valid lab report.");
            mr.setTests(new HashMap<>());
            return mr;
        }

        // 4. Parse structured tests
        Map<String, TestResult> testMap = new HashMap<>();

        for (Map.Entry<String, String> e : fields.entrySet()) {
            TestResult tr = new TestResult();
            tr.setName(e.getKey());
            tr.setRawValue(e.getValue());
            try {
                String num = e.getValue().replaceAll("[^0-9.\\-]", "");
                if (!num.isBlank()) tr.setValue(Double.parseDouble(num));
            } catch (Exception ignored) {}
            testMap.put(e.getKey(), tr);
        }

        // AI-friendly map
        Map<String, String> simpleTests = new HashMap<>();
        for (Map.Entry<String, TestResult> e : testMap.entrySet()) {
            simpleTests.put(e.getKey(), e.getValue().getRawValue());
        }

        // 5. AI summarization
        String aiOutput = aiService.summarizeAndInterpret(cleanedText, simpleTests);

        // Log raw AI output for debugging
        System.out.println("======= RAW AI OUTPUT =======");
        System.out.println(aiOutput);
        System.out.println("=============================");

        // 6. extract summary safely
        String summary = extractSummary(aiOutput);

        // 7. fill report
        mr.setDocType(type.name());
        mr.setText(cleanedText);
        mr.setTests(testMap);
        mr.setAiInterpretation(aiOutput);
        mr.setSummary(summary);

        // 8. generate PDF
        byte[] pdf = pdfService.generateReportPdf(
                "AI Doctor - Clinical Report",
                cleanedText,
                simpleTests,
                aiOutput
        );
        mr.setPdfBytes(pdf);

        return mr;
    }

    /**
     * Ultra-robust JSON + plain-text summary extractor.
     */
    private String extractSummary(String aiReply) {
        if (aiReply == null || aiReply.isBlank()) return "No summary available";

        try {
            // Remove markdown fences
            String cleaned = aiReply.replace("```json", "")
                    .replace("```", "")
                    .trim();

            // Try strict JSON
            JsonNode root = mapper.readTree(cleaned);
            if (root.has("summary")) {
                String summary = root.get("summary").asText();
                if (!summary.isBlank()) return summary;
            }
        } catch (Exception ignored) {}

        // Look for "summary:" text lines
        for (String line : aiReply.split("\n")) {
            if (line.toLowerCase().contains("summary")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2 && !parts[1].isBlank()) {
                    return parts[1].trim();
                }
            }
        }

        // fallback: first meaningful sentence
        for (String line : aiReply.split("\n")) {
            if (line.trim().length() > 30) return line.trim();
        }

        // final fallback
        return "No summary available";
    }
}
