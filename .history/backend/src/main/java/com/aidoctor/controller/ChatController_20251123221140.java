package com.aidoctor.controller;

import com.aidoctor.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AiService aiService;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping(path = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter start(@RequestParam(name = "username", required = false, defaultValue = "Patient") String username) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send("...typing");
        } catch (Exception ignored) {}

        pool.submit(() -> {
            try {
                String greeting = aiService.initialGreeting(username);
                emitter.send(greeting);
                emitter.complete();
            } catch (Exception e) {
                try { emitter.send("{\"error\":\"" + e.getMessage() + "\"}"); } catch (Exception ignore) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String,Object> body) {
        String userMessage = String.valueOf(body.getOrDefault("message",""));
        SseEmitter emitter = new SseEmitter(0L);
        try { emitter.send("...typing"); } catch (Exception ignored) {}

        pool.submit(() -> {
            try {
                String reply = aiService.chatWithDoctor(userMessage);
                emitter.send(reply);
                emitter.complete();
            } catch (Exception e) {
                try { emitter.send("{\"error\":\""+ e.getMessage() +"\"}"); } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
