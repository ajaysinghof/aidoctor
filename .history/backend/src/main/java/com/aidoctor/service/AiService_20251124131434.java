package com.aidoctor.service;

import org.springframework.stereotype.Service;

/**
 * Minimal AiService that provides a simple echo-style reply so /api/chat works.
 * You can later replace with real OpenAI calls.
 */
@Service
public class AiService {

    public String simpleChat(String userText) {
        if (userText == null || userText.isBlank()) return "Hello — how can I help?";
        // Simple canned logic:
        if (userText.toLowerCase().contains("hello") || userText.toLowerCase().contains("hi")) {
            return "Hi there — I'm Dr. Raghav. Tell me your symptoms.";
        }
        return "You said: " + userText + " — (This is a simple local reply.)";
    }

    // If other code expects these names in the future, keep stubs:
    public String fixBrokenMedicalText(String t) { return t == null ? "" : t; }
    public Object classifyDocumentWithAi(String t) { return null; }
    public String summarizeAndInterpret(String text, java.util.Map<String,String> tests) { return "No interpretation available."; }
}
