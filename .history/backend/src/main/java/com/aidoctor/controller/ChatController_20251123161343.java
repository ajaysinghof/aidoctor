package com.aidoctor.controller;

import com.aidoctor.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String message = body.get("text");
        String aiReply = chatService.ask(message);
        return Map.of("reply", aiReply);
    }
}
