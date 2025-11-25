package com.aidoctor.model;

public class ChatRequest {
    private String userId;
    private String text;

    public ChatRequest() {}

    public ChatRequest(String userId, String text) {
        this.userId = userId;
        this.text = text;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
