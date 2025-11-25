package com.aidoctor.controller;

import com.aidoctor.model.ChatMessage;
import com.aidoctor.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessage request) {
        return ResponseEntity.ok(chatService.handleMessage(request));
    }
}
