package com.aidoctor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String openaiKey;

    public String askOpenAI(String prompt) {
        RestTemplate rest = new RestTemplate();
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openaiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String,Object> payload = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 1000
        );

        HttpEntity<Map<String,Object>> req = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = rest.postForEntity(url, req, Map.class);

        try {
            Map choices = (Map) ((List) response.getBody().get("choices")).get(0);
            Map message = (Map) choices.get("message");
            return message.get("content").toString();
        } catch (Exception ex) {
            return "AI Error: " + ex.getMessage();
        }
    }
}
