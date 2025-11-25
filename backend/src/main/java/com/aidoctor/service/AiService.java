package com.aidoctor.service;

import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final OpenAIService openAIService;

    public AiService(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    /**
     * Real AI chat powered by OpenAI
     */
    public String simpleChat(String userText) {

        if (userText == null || userText.isBlank()) {
            return "Hello, I'm Dr. Raghav 👨‍⚕️.\nHow can I help you today?";
        }

        // friendly greeting without OpenAI
        if (userText.toLowerCase().contains("hello") ||
                userText.toLowerCase().contains("hi") ||
                userText.toLowerCase().contains("hey")) {
            return "Hi, I'm Dr. Raghav 👨‍⚕️.\nYou can ask me anything or upload your medical report.";
        }

        // SYSTEM prompt
        String systemPrompt = """
                You are an expert medical doctor.
                Give simple, accurate medical explanations.
                Always provide:
                - explanation
                - symptoms
                - possible causes
                - recommended next steps
                """;

        // USER prompt
        String userPrompt =
                "Interpret this medical message or question:\n\n" + userText;

        return openAIService.askOpenAI(systemPrompt, userPrompt);
    }

    /** Called from OCR pipeline - AI fixes broken text */
    public String fixBrokenMedicalText(String text) {

        String systemPrompt = "You are a medical OCR correction assistant.";
        String userPrompt = "Fix and clean this OCR text:\n\n" + text;

        return openAIService.askOpenAI(systemPrompt, userPrompt);
    }

    /** AI classification of document type */
    public String classifyDocumentWithAi(String text) {

        String systemPrompt = "You classify documents into: Prescription, Lab Report, Diagnostic Report, Form, Other.";
        String userPrompt = "Classify this document:\n\n" + text;

        return openAIService.askOpenAI(systemPrompt, userPrompt);
    }

    /** AI medical interpretation of report */
    public String summarizeAndInterpret(String text, java.util.Map<String, String> tests) {

        String systemPrompt = """
                You are a senior medical doctor.
                Summarize the report and give findings, conditions, and recommendations.
                Output JSON only.
                """;

        String userPrompt =
                "Extracted Text:\n" + text +
                "\n\nTests Found:\n" + tests.toString();

        return openAIService.askOpenAI(systemPrompt, userPrompt);
    }
}
