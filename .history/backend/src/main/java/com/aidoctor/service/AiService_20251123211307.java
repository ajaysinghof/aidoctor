package com.aidoctor.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiService {

    private final OpenAiClient openAi; // use your real client

    public AiService(OpenAiClient openAi) {
        this.openAi = openAi;
    }

    // -------------------------
    // 1. Fix broken OCR text
    // -------------------------
    public String fixBrokenMedicalText(String text) {
        String prompt = "Fix OCR errors in the following medical text. Only correct text, do not add anything:\n\n" + text;

        return openAi.simpleText(prompt);
    }

    // -------------------------
    // 2. AI classification fallback
    // -------------------------
    public DocumentClassifier.DocType classifyDocumentWithAi(String text) {
        String prompt = "Is this a medical document? Reply ONLY with: MEDICAL or NON_MEDICAL.\n\n" + text;

        String response = openAi.simpleText(prompt).trim().toUpperCase();

        if (response.contains("MEDICAL")) return DocumentClassifier.DocType.MEDICAL;
        return DocumentClassifier.DocType.NON_MEDICAL;
    }

    // -------------------------
    // 3. Original summarization
    // -------------------------
    public String summarizeAndInterpret(String cleanedText, Map<String, String> tests) {
        String prompt = "Summarize and interpret the following medical report text and test values.\n\n" +
                "Text:\n" + cleanedText + "\n\n" +
                "Extracted Test Values:\n" + tests.toString() + "\n\n" +
                "Respond in JSON with fields: summary, findings, interpretation, recommendations.";

        return openAi.simpleText(prompt);
    }

    // -------------------------
    // 4. NEW overload with doctorSummary
    // -------------------------
    public String summarizeAndInterpret(String cleanedText, Map<String, String> tests, String doctorSummary) {
        String prompt = "You are a medical doctor. Merge the structured doctor findings with your own analysis.\n\n" +
                "OCR Text:\n" + cleanedText + "\n\n" +
                "Extracted Test Values:\n" + tests.toString() + "\n\n" +
                "Doctor Summary:\n" + doctorSummary + "\n\n" +
                "Produce a clean JSON with fields: summary, detailed_interpretation, recommendations.";

        return openAi.simpleText(prompt);
    }

    // -------------------------
    // 5. New AI helper: interpret numeric test
    // -------------------------
    public String interpretNumericTest(String name, double value, String unit) {
        String prompt = "Interpret this medical test value like a doctor.\n" +
                "Test: " + name + "\n" +
                "Value: " + value + " " + (unit == null ? "" : unit) + "\n" +
                "Explain what it means in 1 short sentence.";

        return openAi.simpleText(prompt);
    }

    // -------------------------
    // 6. New AI helper: interpret raw text test value
    // -------------------------
    public String interpretRawTest(String name, String raw) {
        String prompt = "Interpret the following medical test result:\n\n" +
                "Test Name: " + name + "\n" +
                "Raw Value: " + raw + "\n\n" +
                "Explain meaning in 1 short sentence.";

        return openAi.simpleText(prompt);
    }
}
