package com.aidoctor.service;

import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiClient {

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();

    public String simpleChat(String message) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", "gpt-3.5-turbo");
            body.put("messages", new JSONObject[]{
                new JSONObject().put("role", "user").put("content", message)
            });

            Request req = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")
                    ))
                    .build();

            Response res = client.newCall(req).execute();
            String resp = res.body().string();

            JSONObject json = new JSONObject(resp);
            return json.getJSONArray("choices")
                       .getJSONObject(0)
                       .getJSONObject("message")
                       .getString("content");

        } catch (Exception e) {
            return "Error contacting AI service: " + e.getMessage();
        }
    }
}
