package com.aidoctor.controller;

import com.aidoctor.model.ChatRequest;
import com.aidoctor.model.ChatResponse;
import com.aidoctor.service.AiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    private final AiService aiService;

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req) {
        String reply = aiService.simpleChat(req.getText());
        return new ChatResponse(reply);
    }
}
