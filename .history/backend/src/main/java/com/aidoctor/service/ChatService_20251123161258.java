package com.aidoctor.service;

import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();

    public String ask(String message) {
        try {
            MediaType JSON = MediaType.parse("application/json");

            String payload = """
                {
                  "model": "gpt-4o-mini",
                  "messages": [
                    {
                      "role": "system",
                      "content": "You are Dr. Raghav, a senior physician. You must give safe, simple, medically accurate advice. Never give treatment without disclaimer. Always encourage consulting a real doctor for urgent cases."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ]
                }
                """.formatted(message);

            RequestBody body = RequestBody.create(payload, JSON);

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                return "❌ AI server error: " + response.code();
            }

            String json = response.body().string();
            JSONObject obj = new JSONObject(json);

            return obj.getJSONArray("choices")
                      .getJSONObject(0)
                      .getJSONObject("message")
                      .getString("content");

        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }
}
