package com.aidoctor.controller;

import com.aidoctor.service.TranscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transcribe")
public class TranscriptionController {

    @Autowired
    private TranscriptionService transcriptionService;

    @PostMapping
    public ResponseEntity<?> transcribe(@RequestParam("audio") MultipartFile audio) {
        String text = transcriptionService.transcribeAudio(audio);
        return ResponseEntity.ok(java.util.Map.of("transcript", text));
    }
}
