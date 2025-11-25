package com.aidoctor.service;

import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final OpenAiClient openAiClient;

    public AiService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public String simpleChat(String message) {
        return openAiClient.simpleChat(message);
    }

    public String summarizeText(String text) {
        return openAiClient.simpleChat("Summarize this medical text:\n\n" + text);
    }
}
