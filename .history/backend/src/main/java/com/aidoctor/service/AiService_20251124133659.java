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

        // If user says hello → small friendly answer (no OpenAI cost)
        if (userText.toLowerCase().contains("hello") ||
                userText.toLowerCase().contains("hi") ||
                userText.toLowerCase().contains("hey")) {
            return "Hi, I'm Dr. Raghav 👨‍⚕️.\nYou can ask me anything or upload your medical report.";
        }

        // Otherwise → send to OpenAI
        String prompt =
                "You are an expert medical doctor. Interpret the following message or medical text:\n\n"
                + userText +
                "\n\nGive clean explanation, symptoms, possible causes, and recommended next steps.";

        return openAIService.askOpenAI(prompt);
    }

    /** Called from OCR pipeline - AI fixes broken text */
    public String fixBrokenMedicalText(String text) {
        String prompt = "Fix the following OCR medical text. Clean it and reconstruct missing words:\n\n" + text;
        return openAIService.askOpenAI(prompt);
    }

    /** AI classification of document type */
    public String classifyDocumentWithAi(String text) {
        String prompt = "Classify the following document into one of: Prescription, Lab Report, Diagnostic Report, Form, Other:\n\n" + text;
        return openAIService.askOpenAI(prompt);
    }

    /** AI medical interpretation of report */
    public String summarizeAndInterpret(String text, java.util.Map<String,String> tests) {
        String prompt =
                "You are an expert medical doctor.\n" +
                "Here is the extracted medical report text:\n\n" +
                text +
                "\n\nHere are the recognized test results (name:value):\n" +
                tests.toString() +
                "\n\nProvide a JSON with fields:\n" +
                "{ \"summary\": \"...\", \"findings\": [...], \"possible_conditions\": [...], \"doctor_recommendations\": [...] }";

        return openAIService.askOpenAI(prompt);
    }
}
