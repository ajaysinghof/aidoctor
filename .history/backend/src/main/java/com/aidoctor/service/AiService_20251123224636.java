package com.aidoctor.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.StringJoiner;

/**
 * AiService: will call OpenAI chat completion if OPENAI_API_KEY env var is set.
 * Otherwise returns canned/heuristic responses so the app still works locally.
 */
@Service
public class AiService {

    private final String openaiKey;
    private final OkHttpClient httpClient = new OkHttpClient();

    public AiService() {
        // prefer environment variable OPENAI_API_KEY; also allow spring property openai.api.key
        String k = System.getenv("OPENAI_API_KEY");
        if (k == null || k.isBlank()) {
            k = System.getProperty("openai.api.key");
        }
        if (k == null || k.isBlank()) {
            k = System.getenv("OPENAI_API_KEY"); // just in case
        }
        this.openaiKey = k;
    }

    public String fixBrokenMedicalText(String brokenText) {
        if (brokenText == null || brokenText.isBlank()) return brokenText;
        String prompt = "You are a clinical text fixer. Clean OCR'd medical text to readable text. " +
                "Return only the corrected text, preserve tests and values when possible.\n\n" + brokenText;
        return callOpenAiOrFallback(prompt, "Fixed text not available");
    }

    public DocumentClassifier.DocType classifyDocumentWithAi(String text) {
        if (text == null || text.isBlank()) return DocumentClassifier.DocType.UNKNOWN;
        String prompt = "Classify the following text as MEDICAL or NON_MEDICAL or UNKNOWN. " +
                "Respond with only the single word MEDICAL, NON_MEDICAL or UNKNOWN.\n\n" + text;
        String resp = callOpenAiOrFallback(prompt, "UNKNOWN").trim().toUpperCase();
        try {
            return DocumentClassifier.DocType.valueOf(resp.split("\\s+")[0]);
        } catch (Exception e) {
            return DocumentClassifier.DocType.UNKNOWN;
        }
    }

    public String summarizeAndInterpret(String cleanedText, Map<String, String> tests) {
        StringJoiner sj = new StringJoiner("\n");
        sj.add("You are a clinician. Given the text and the following extracted tests (key:value),");
        sj.add("produce a concise JSON object with fields: summary (doctor one-liner), findings (list), recommendations (list).");
        sj.add("If a value is missing or cannot be interpreted, say so.");
        sj.add("\nText:\n" + (cleanedText == null ? "" : cleanedText));
        sj.add("\nTests:");
        for (Map.Entry<String, String> e : tests.entrySet()) {
            sj.add(e.getKey() + " : " + e.getValue());
        }
        sj.add("\nReturn only JSON.");
        return callOpenAiOrFallback(sj.toString(), "{\"summary\":\"No summary available\",\"findings\":[],\"recommendations\":[]}");
    }

    public String interpretNumericTest(String name, double value, String unit) {
        String prompt = "Interpret this single lab value for a patient in plain english. " +
                "Test: " + name + "\nValue: " + value + " " + (unit == null ? "" : unit) +
                "\nRespond in one short sentence like 'Hemoglobin is low' or 'WBC normal'.";
        return callOpenAiOrFallback(prompt, name + " interpretation unavailable");
    }

    public String interpretRawTest(String name, String rawValue) {
        String prompt = "Interpret this lab test raw value for a clinician. " +
                "Test: " + name + "\nRaw: " + rawValue + "\nGive a concise interpretation.";
        return callOpenAiOrFallback(prompt, name + " interpretation unavailable");
    }

    private String callOpenAiOrFallback(String prompt, String fallback) {
        if (openaiKey == null || openaiKey.isBlank()) {
            // fallback behaviour: return short canned outputs or provide heuristics
            if (prompt.length() < 600) return fallback;
            // naive attempt to extract a one-line summary from prompt text
            String t = prompt.replaceAll("\\s+", " ");
            if (t.length() > 200) return "{\"summary\":\"No summary available\",\"findings\":[],\"recommendations\":[]}";
            return fallback;
        }

        // Call OpenAI Chat Completion (gpt-3.5-turbo style) via HTTP
        try {
            JSONObject body = new JSONObject();
            JSONArray messages = new JSONArray();
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", "You are a helpful clinician assistant.");
            messages.put(system);
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", prompt);
            messages.put(user);
            body.put("model", "gpt-3.5-turbo");
            body.put("messages", messages);
            body.put("max_tokens", 800);

            Request req = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + openaiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) return fallback;
                String respBody = resp.body().string();
                JSONObject jo = new JSONObject(respBody);
                JSONArray choices = jo.optJSONArray("choices");
                if (choices != null && choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject msg = choice.optJSONObject("message");
                    if (msg != null) {
                        return msg.optString("content", fallback);
                    }
                }
                return fallback;
            }
        } catch (IOException e) {
            return fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }
}
