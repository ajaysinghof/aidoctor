package com.aidoctor.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class AiService {

    private final OpenAiClient openAiClient;

    public AiService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public String fixBrokenMedicalText(String broken) {
        if (broken == null || broken.isBlank()) return broken;

        String prompt =
                "Fix OCR medical text. Output clean text only.\n\n" + broken;

        return openAiClient.simpleChat(prompt);
    }

    public DocumentClassifier.DocType classifyDocumentWithAi(String text) {
        String prompt =
                "Classify this document as MEDICAL, NON_MEDICAL, or UNKNOWN.\n" +
                "Return ONLY the single word.\n\n" + text;

        String r = openAiClient.simpleChat(prompt).trim().toUpperCase();

        try {
            return DocumentClassifier.DocType.valueOf(r);
        } catch (Exception e) {
            return DocumentClassifier.DocType.UNKNOWN;
        }
    }

    public String summarizeAndInterpret(String cleanedText, Map<String, String> tests) {

        StringJoiner sj = new StringJoiner("\n");
        sj.add("You are an expert medical AI. STRICTLY return VALID JSON ONLY.");
        sj.add("Format:");
        sj.add("{\"summary\":\"...\",\"findings\":[\"...\"],\"recommendations\":[\"...\"]}");
        sj.add("NO markdown. NO commentary.");

        sj.add("\nMedical Text:\n" + cleanedText);
        sj.add("\nExtracted Tests:");
        tests.forEach((k, v) -> sj.add(k + ": " + v));

        String out = openAiClient.simpleChat(sj.toString());

        // Debug log
        System.out.println("=== AI SUMMARY RAW OUTPUT ===");
        System.out.println(out);
        System.out.println("=============================");

        return out;
    }

    public String interpretNumericTest(String name, double value, String unit) {
        String prompt =
                "Interpret this medical test numeric value. One concise sentence.\n" +
                "Test: " + name + "\nValue: " + value + " " + unit;

        return openAiClient.simpleChat(prompt);
    }

    public String interpretRawTest(String name, String raw) {
        String prompt =
                "Interpret this medical test raw value in one doctor sentence.\n" +
                "Test: " + name + "\nRaw: " + raw;

        return openAiClient.simpleChat(prompt);
    }
}
