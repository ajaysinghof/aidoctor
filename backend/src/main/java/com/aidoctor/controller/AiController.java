package com.aidoctor.controller;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Value("${openai.api.key}")
    private String openaiKey;

    @PostMapping("/explain")
    public ResponseEntity<?> explain(@RequestBody Map<String,Object> body){
        // body expected: { "summary": "extracted summary", "tests": {...} }
        if(openaiKey==null || openaiKey.isEmpty()) return ResponseEntity.status(400).body(Map.of("error","OpenAI key not set"));
        String prompt = "You are a helpful doctor. Given these test results and summary, produce a readable explanation and recommendations for a patient:\n\n" + body.toString();

        RestTemplate rest = new RestTemplate();
        String url = "https://api.openai.com/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openaiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String,Object> payload = Map.of(
                "model","gpt-4o-mini",
                "messages", List.of(Map.of("role","user","content",prompt)),
                "max_tokens",500
        );

        HttpEntity<Map<String,Object>> req = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<Map> resp = rest.postForEntity(url, req, Map.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (Exception ex){
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }
}
