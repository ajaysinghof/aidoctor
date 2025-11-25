package com.aidoctor.controller;

import com.aidoctor.service.OcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
public class ReportController {

    private final OcrService ocrService;

    public ReportController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        try {
            String text = ocrService.extractText(file);

            return ResponseEntity.ok()
                    .body(new OcrResponse(text));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                new ErrorResponse("OCR failed: " + e.getMessage())
            );
        }
    }

    // -------------------
    // RESPONSES
    // -------------------
    record OcrResponse(String summary) {}
    record ErrorResponse(String message) {}
}
