package com.aidoctor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAIService {

    @Value("${app.openai-api-key:}")
    private String openaiApiKey;

    public String callChatModel(String prompt) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            return "MOCK OPENAI RESPONSE: OPENAI_API_KEY not configured. Prompt: " + (prompt.length() > 200 ? prompt.substring(0,200)+"..." : prompt);
        }
        // TODO: implement real HTTP call to OpenAI here
        return "(placeholder)"; 
    }
}
