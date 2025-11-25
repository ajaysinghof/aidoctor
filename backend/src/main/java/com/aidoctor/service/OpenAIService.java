package com.aidoctor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Simple OpenAI caller using RestTemplate --> Chat Completions.
 * Reads openai.api.key from application.properties or env OPENAI_API_KEY.
 */
@Service
public class OpenAIService {

    @Value("${openai.api.key:}")
    private String openaiKeyProp;

    private String getApiKey() {
        if (openaiKeyProp != null && !openaiKeyProp.isBlank()) return openaiKeyProp;
        String env = System.getenv("OPENAI_API_KEY");
        return env == null ? "" : env;
    }

    /**
     * Sends a prompt to OpenAI and returns the assistant's content as String.
     * Returns helpful error messages if key missing or OpenAI fails.
     */
    public String askOpenAI(String systemPrompt, String userPrompt) {
        String key = getApiKey();
        if (key == null || key.isBlank()) {
            return "AI not configured: OPENAI_API_KEY missing";
        }

        RestTemplate rest = new RestTemplate();
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(key);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build the messages: system + user
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        Map<String, Object> payload = Map.of(
                "model", "gpt-4o-mini",
                "messages", messages,
                "max_tokens", 800
        );

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<Map> resp = rest.postForEntity(url, req, Map.class);
            Map body = resp.getBody();
            if (body == null) return "AI returned empty response";
            List choices = (List) body.get("choices");
            if (choices == null || choices.isEmpty()) return "AI returned no choices";
            Map choice0 = (Map) choices.get(0);
            // try both 'message' and 'text' shapes
            Object messageObj = choice0.get("message");
            if (messageObj instanceof Map) {
                Map msg = (Map) messageObj;
                Object content = msg.get("content");
                return content == null ? "AI returned empty content" : content.toString();
            } else if (choice0.get("text") != null) {
                return choice0.get("text").toString();
            } else {
                return "AI returned an unexpected shape";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "AI request failed: " + ex.getMessage();
        }
    }
}
