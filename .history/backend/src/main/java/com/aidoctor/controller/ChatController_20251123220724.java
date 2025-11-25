package com.aidoctor.controller;

import com.aidoctor.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AiService aiService;

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    // FIRST MESSAGE — Greeting + 2 Options
    @GetMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> startChat(@RequestParam String username) {

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // Emit "Doctor is typing…"
        sink.tryEmitNext("...typing");

        // Ask ChatGPT for greeting
        String full = aiService.initialGreeting(username);

        // Emit final message
        sink.tryEmitNext(full);
        sink.tryEmitComplete();

        return sink.asFlux();
    }

    // STREAMED CHAT
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody Map<String, String> body) {

        String userMessage = body.get("message");

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 1. Immediately show typing
        sink.tryEmitNext("...typing");

        // 2. Now async compute
        new Thread(() -> {
            try {
                String answer = aiService.chatWithDoctor(userMessage);
                sink.tryEmitNext(answer);
            } catch (Exception e) {
                sink.tryEmitNext("Error: " + e.getMessage());
            } finally {
                sink.tryEmitComplete();
            }
        }).start();

        return sink.asFlux();
    }
}
