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

    // crude "broken OCR" detector (same as you had)
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
            try {
                type = aiService.classifyDocumentWithAi(cleanedText);
            } catch (Exception ignored) {
                type = DocumentClassifier.DocType.UNKNOWN;
            }
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
        if (fields != null) {
            for (Map.Entry<String, String> e : fields.entrySet()) {
                String name = e.getKey().trim();
                String raw = e.getValue() == null ? "" : e.getValue().trim();
                TestResult tr = new TestResult();
                tr.setName(name);
                tr.setRawValue(raw);
                // basic numeric extraction - crude
                try {
                    String numOnly = raw.replaceAll("[^0-9.\\-]", "");
                    if (!numOnly.isBlank()) tr.setValue(Double.parseDouble(numOnly));
                } catch (Exception ignored) {
                }
                testsMap.put(name, tr);
            }
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
                    } catch (Exception ignored) {
                    }
                    testsMap.put(key, tr);
                }
            }
        }

        // 7) If some tests still missing numeric parse, ask AI to interpret
        for (Map.Entry<String, TestResult> en : testsMap.entrySet()) {
            TestResult tr = en.getValue();
            if (tr.getValue() == null || tr.getValue().isNaN()) {
                // fallback to aiService interpretRawTest for an interpretation string
                try {
                    String interp = aiService.interpretRawTest(tr.getName(), tr.getRawValue());
                    tr.setInterpretation(interp);
                } catch (Exception ignored) {
                }
            } else {
                // numeric interpretation
                try {
                    String u = tr.getUnit() == null ? "" : tr.getUnit();
                    String interp = aiService.interpretNumericTest(tr.getName(), tr.getValue(), u);
                    tr.setInterpretation(interp);
                } catch (Exception ignored) {
                }
            }
        }

        // 8) Ask AI for interpretation and summary (pass cleanedText and structured tests)
        Map<String, String> simpleTests = new HashMap<>();
        for (Map.Entry<String, TestResult> en : testsMap.entrySet()) {
            simpleTests.put(en.getKey(), en.getValue().getRawValue() == null ? "" : en.getValue().getRawValue());
        }

        String aiJson = "";
        try {
            aiJson = aiService.summarizeAndInterpret(cleanedText, simpleTests);
        } catch (Exception ex) {
            aiJson = "AI summary failed: " + ex.getMessage();
        }

        // 9) set fields on MedicalReport
        mr.setText(cleanedText);
        mr.setDocType(type.name());
        mr.setTests(testsMap);
        mr.setAiInterpretation(aiJson);
        mr.setSummary(extractSummaryFromAiReply(aiJson));

        // 10) Also generate a clean PDF and attach raw bytes
        byte[] pdf = pdfService.generateReportPdf("AI Doctor - Corrected Report", cleanedText, simpleTests, mr.getAiInterpretation());
        mr.setPdfBytes(pdf);

        return mr;
    }

    // crude parser to extract "summary" field from aiJson (which may be JSON or text)
    private String extractSummaryFromAiReply(String aiReply) {
        if (aiReply == null) return "";
        // try to find "summary" key in JSON-like reply
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
        // fallback: try to return first line or first 300 chars
        String trimmed = aiReply.trim();
        if (trimmed.contains("\n")) return trimmed.split("\\r?\\n")[0];
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }
}
