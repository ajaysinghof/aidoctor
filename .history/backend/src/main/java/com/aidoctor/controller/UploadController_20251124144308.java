package com.aidoctor.controller;

import com.aidoctor.service.OcrService;
import com.aidoctor.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin
public class UploadController {

    private final OcrService ocrService;
    private final ReportService reportService;

    public UploadController(OcrService ocrService, ReportService reportService) {
        this.ocrService = ocrService;
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
            }

            // Step 1 → Extract OCR text
            String extractedText = ocrService.extractText(file);

            // Step 2 → Run medical classification + AI summary
            Map<String, Object> result =
                    reportService.processAndInterpret(file.getOriginalFilename(), extractedText);

            // Step 3 → return JSON
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
