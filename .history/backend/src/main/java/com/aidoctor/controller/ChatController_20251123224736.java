package com.aidoctor.controller;

import com.aidoctor.model.ChatRequest;
import com.aidoctor.model.ChatResponse;
import com.aidoctor.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AiService aiService;

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/message")
    public ResponseEntity<?> message(@RequestBody ChatRequest req) {
        try {
            String userText = req.getText() == null ? "" : req.getText().trim();

            // initial greeting branch example:
            if (userText.equalsIgnoreCase("hi") || userText.equalsIgnoreCase("hello") || userText.equalsIgnoreCase("hey")) {
                String greeting = "Hi " + (req.getUserId() == null ? "Patient" : req.getUserId()) + ". Do you want to (1) Chat with doctor or (2) Show/upload a report? Reply with 'chat' or 'report'.";
                return ResponseEntity.ok(new ChatResponse(greeting));
            }

            // if user asks to chat:
            if (userText.equalsIgnoreCase("chat") || userText.toLowerCase().contains("chat with doctor")) {
                String reply = "You chose to chat with doctor. How can I help? (Describe symptoms or upload a report)";
                return ResponseEntity.ok(new ChatResponse(reply));
            }

            // if user asks 'report' or 'upload'
            if (userText.equalsIgnoreCase("report") || userText.toLowerCase().contains("upload")) {
                String reply = "You chose report. Please upload the report using the 'Upload Medical Report' UI or POST /api/ocr/extract multipart/form-data file field 'file'.";
                return ResponseEntity.ok(new ChatResponse(reply));
            }

            // Otherwise, treat as general question -> forward to AI (if available) or return canned response.
            // Simulate "Doctor typing" by returning a short "typing" message first (client can show typing)
            String typing = "Doctor is typing..."; // client can show this immediately
            // call AI to get reply
            String aiReply = aiService.fixBrokenMedicalText(""); // dummy quick call to ensure service present
            // Actually send the user message for interpretation:
            String finalReply = aiService.summarizeAndInterpret(userText, java.util.Collections.emptyMap());
            // If the response is JSON, attempt to extract summary; else return raw
            return ResponseEntity.ok(new ChatResponse(finalReply));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ChatResponse("❌ Error processing chat: " + ex.getMessage()));
        }
    }
}
