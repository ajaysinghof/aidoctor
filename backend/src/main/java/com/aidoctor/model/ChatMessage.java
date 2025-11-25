package com.aidoctor.model;

public class ChatMessage {

    private String userId;
    private String text;
    private String[] attachmentIds;

    public ChatMessage() {}

    public String getUserId() { return userId; }
    public String getText() { return text; }
    public String[] getAttachmentIds() { return attachmentIds; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setText(String text) { this.text = text; }
    public void setAttachmentIds(String[] attachmentIds) { this.attachmentIds = attachmentIds; }
}
