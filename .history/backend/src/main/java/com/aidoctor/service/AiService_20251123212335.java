package com.aidoctor.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.StringJoiner;

/**
 * AI helper service: uses OpenAiClient to perform small tasks:
 * - fixBrokenMedicalText
 * - classifyDocumentWithAi
 * - summarizeAndInterpret
 * - interpretNumericTest
 * - interpretRawTest
 *
 * These methods produce text interpretations. They are intentionally simple
 * (string-based). You can later parse JSON if you want structured outputs.
 */
@Service
public class AiService {

    private final OpenAiClient openAiClient;

    public AiService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public String fixBrokenMedicalText(String brokenText) {
        if (brokenText == null || brokenText.isBlank()) return brokenText;
        String prompt = "You are a clinical text fixer. Clean OCR'd medical text to readable text. " +
                "Return only the corrected text, preserve tests and values when possible.\n\n" + brokenText;
        return openAiClient.simpleChat(prompt);
    }

    public com.aidoctor.service.DocumentClassifier.DocType classifyDocumentWithAi(String text) {
        if (text == null || text.isBlank()) return com.aidoctor.service.DocumentClassifier.DocType.UNKNOWN;
        String prompt = "Classify the following text as MEDICAL or NON_MEDICAL or UNKNOWN. " +
                "Respond with only the single word MEDICAL, NON_MEDICAL or UNKNOWN.\n\n" + text;
        String resp = openAiClient.simpleChat(prompt).trim().toUpperCase();
        try {
            return com.aidoctor.service.DocumentClassifier.DocType.valueOf(resp);
        } catch (Exception e) {
            return com.aidoctor.service.DocumentClassifier.DocType.UNKNOWN;
        }
    }

    /**
     * Summarize and interpret lab tests provided.
     * Returns a textual explanation (doctor-style). You can change the prompt to return JSON.
     */
    public String summarizeAndInterpret(String cleanedText, Map<String, String> tests) {
        StringJoiner sj = new StringJoiner("\n");
        sj.add("You are a helpful clinician. Given the text and the following extracted tests (key:value),");
        sj.add("produce a concise JSON object with fields: summary (doctor one-liner), findings (list), recommendations (list).");
        sj.add("If a value is missing or cannot be interpreted, say so.");
        sj.add("\nText:\n" + (cleanedText == null ? "" : cleanedText));
        sj.add("\nTests:");
        for (Map.Entry<String, String> e : tests.entrySet()) {
            sj.add(e.getKey() + " : " + e.getValue());
        }
        sj.add("\nReturn only JSON.");

        String out = openAiClient.simpleChat(sj.toString());
        return out;
    }

    public String interpretNumericTest(String name, double value, String unit) {
        String prompt = "Interpret this single lab value for a patient in plain english. " +
                "Test: " + name + "\nValue: " + value + " " + (unit == null ? "" : unit) +
                "\nRespond in one short sentence like 'Hemoglobin is low' or 'WBC normal'.";
        return openAiClient.simpleChat(prompt);
    }

    public String interpretRawTest(String name, String rawValue) {
        String prompt = "Interpret this lab test raw value for a clinician. " +
                "Test: " + name + "\nRaw: " + rawValue + "\nGive a concise interpretation.";
        return openAiClient.simpleChat(prompt);
    }
}
