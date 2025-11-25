package com.aidoctor.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiClient {

    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey;

    public OpenAiClient(@Value("${openai.api.key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing openai.api.key in application.properties");
        }
        this.apiKey = apiKey;
    }

    public String simpleChat(String prompt) {
        try {
            MediaType JSON = MediaType.parse("application/json");

            String payload = """
            {
              "model": "gpt-3.5-turbo",
              "messages": [{"role":"user","content":"%s"}]
            }
            """.formatted(prompt.replace("\"","'"));

            Request req = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .post(RequestBody.create(payload, JSON))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response res = client.newCall(req).execute();
            return res.body().string();

        } catch (Exception e) {
            e.printStackTrace();
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }
}
