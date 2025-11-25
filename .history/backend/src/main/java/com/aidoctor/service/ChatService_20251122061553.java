package com.aidoctor.service;

import com.aidoctor.model.ChatMessage;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    public Object handleMessage(ChatMessage request) {
        StringBuilder response = new StringBuilder();
        response.append("Received: ").append(request.getText() == null ? "" : request.getText());
        if (request.getAttachmentIds() != null) {
            response.append("\nAttachments: ");
            for (String id : request.getAttachmentIds()) response.append(id).append(" ");
        }
        return response.toString();
    }
}
