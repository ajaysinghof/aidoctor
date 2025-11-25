package com.aidoctor.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * Small OpenAI client built on OkHttp.
 * - reads OPENAI_API_KEY from environment (or spring property if you change)
 * - provides simpleChat(prompt) returning assistant reply as plain text
 *
 * NOTE: Adjust model to your account (gpt-3.5-turbo is safe).
 */
@Component
public class OpenAiClient {

    private final OkHttpClient client;
    private final String apiKey;
    private final String model;

    public OpenAiClient() {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY not set in environment");
        }
        this.model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-3.5-turbo");
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(120))
                .build();
    }

    /**
     * Simple single-prompt chat: returns assistant reply string.
     */
    public String simpleChat(String prompt) {
        JSONObject root = new JSONObject();
        root.put("model", model);

        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", "You are a helpful clinical assistant. Be concise but accurate.");
        messages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", prompt);
        messages.put(user);

        root.put("messages", messages);
        root.put("max_tokens", 800);
        root.put("temperature", 0.2);

        RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request req = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .header("Authorization", "Bearer " + apiKey)
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                String err = resp.body() != null ? resp.body().string() : "unknown";
                throw new RuntimeException("OpenAI error: " + resp.code() + " - " + err);
            }
            String respBody = resp.body().string();
            JSONObject j = new JSONObject(respBody);
            JSONArray choices = j.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject first = choices.getJSONObject(0);
                JSONObject msg = first.getJSONObject("message");
                return msg.optString("content", "").trim();
            }
            return "";
        } catch (IOException e) {
            throw new RuntimeException("Failed to call OpenAI: " + e.getMessage(), e);
        }
    }
}
