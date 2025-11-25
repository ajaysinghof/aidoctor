package com.aidoctor.controller;

import com.aidoctor.service.OcrService;
import com.aidoctor.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin // allow frontend from localhost:3000 or configure globally
public class OcrController {

    private final OcrService ocrService;
    private final ReportService reportService;

    @Autowired
    public OcrController(OcrService ocrService, ReportService reportService) {
        this.ocrService = ocrService;
        this.reportService = reportService;
    }

    /**
     * Upload endpoint used by frontend.
     * Returns a JSON object with:
     *  - text: raw extracted text (always present when success)
     *  - isMedical: boolean
     *  - reason: if not medical, string reason
     *  - aiReply: if medical, the OpenAI reply (summary / interpretation)
     */
    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        try {
            // 1) Extract text from file
            String extracted = ocrService.extractText(file);

            // 2) Process + classify + optionally call AI
            Map<String, Object> result = reportService.processAndInterpret(file.getOriginalFilename(), extracted);

            return ResponseEntity.ok(result);
        } catch (IllegalStateException ise) {
            // e.g., missing AWS / Textract not configured or other preconditions
            return ResponseEntity.status(500).body(Map.of("error", ise.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }
}
