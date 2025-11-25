package com.aidoctor.model;

public class ChatRequest {

    private String text;
    private Long userId;

    public ChatRequest() {}

    public ChatRequest(String text, Long userId) {
        this.text = text;
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
