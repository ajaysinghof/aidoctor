package com.aidoctor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${openai.api.key}")
    private String openaiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * Real ChatGPT-powered chat
     */
    public String simpleChat(String userText) {
        if (openaiKey == null || openaiKey.isBlank()) {
            // Fallback if key missing
            return "⚠ OpenAI API key missing — add openai.api.key in application.properties";
        }

        try {
            RestTemplate rest = new RestTemplate();

            // Build request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiKey);

            Map<String, Object> body = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are Dr. Raghav, a friendly medical assistant."),
                            Map.of("role", "user", "content", userText)
                    ),
                    "temperature", 0.7
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // Call ChatGPT
            ResponseEntity<Map> response = rest.exchange(
                    OPENAI_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // Extract response
            try {
                Map choices = ((List<Map>) response.getBody().get("choices")).get(0);
                Map message = (Map) choices.get("message");
                return message.get("content").toString();
            } catch (Exception ex) {
                return "⚠ ChatGPT responded but parsing failed: " + ex.getMessage();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ ChatGPT Error: " + e.getMessage();
        }
    }

    // --- UNUSED STUBS (needed so old code doesn’t break) ---
    public String fixBrokenMedicalText(String t) { return t == null ? "" : t; }
    public Object classifyDocumentWithAi(String t) { return null; }
    public String summarizeAndInterpret(String text, java.util.Map<String,String> tests) { return "AI interpretation disabled."; }
}
