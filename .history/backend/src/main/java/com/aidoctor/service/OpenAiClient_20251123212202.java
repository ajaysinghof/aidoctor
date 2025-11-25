package com.aidoctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Minimal OpenAI client using chat completions (v1).
 * Keeps responses as plain text. Replace model if needed.
 */
@Component
public class OpenAiClient {

    @Value("${openai.api.key:}")
    private String apiKey;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String simpleChat(String prompt) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";

            String payload = """
                    {
                      "model": "gpt-4o-mini",
                      "messages": [{"role":"user","content":"%s"}],
                      "temperature": 0.1,
                      "max_tokens": 800
                    }
                    """.formatted(escapeJson(prompt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> req = new HttpEntity<>(payload, headers);
            ResponseEntity<String> res = rest.exchange(url, HttpMethod.POST, req, String.class);

            JsonNode root = mapper.readTree(res.getBody());
            JsonNode msg = root.path("choices").get(0).path("message").path("content");
            if (msg.isMissingNode()) return res.getBody();
            return msg.asText();
        } catch (Exception e) {
            return "OpenAI error: " + e.getMessage();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", "\\n");
    }
}
