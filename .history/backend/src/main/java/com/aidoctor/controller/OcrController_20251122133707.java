package com.aidoctor.controller;

import com.aidoctor.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ocrService.extractTextAsJson(file));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new ErrorResponse("error", "OCR failed: " + e.getMessage())
            );
        }
    }

    static class ErrorResponse {
        public String status;
        public String message;

        public ErrorResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}
