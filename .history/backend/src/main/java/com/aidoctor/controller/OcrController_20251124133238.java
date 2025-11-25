package com.aidoctor.controller;

import com.aidoctor.service.OcrService;
import com.aidoctor.service.OpenAIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin
public class OcrController {

    private final OcrService ocrService;
    private final OpenAIService openAIService;

    public OcrController(OcrService ocrService, OpenAIService openAIService) {
        this.ocrService = ocrService;
        this.openAIService = openAIService;
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        try {
            // 1️⃣ Extract text
            String text = ocrService.extractText(file);

            // 2️⃣ Automatically send to ChatGPT
            String aiReply = openAIService.askOpenAI("""
                    You are a medical document analyzer.
                    Analyze the extracted text and detect:
                    - Document Type (Prescription, Lab Report, Diagnostic Report, Other)
                    - Patient details (Name, Age, Gender, Doctor, Date)
                    - A table of test values (if available)
                    - A clean rewritten summary

                    Return output strictly in this JSON format:
                    {
                        "document_type": "",
                        "patient_details": {
                            "name": "",
                            "age": "",
                            "gender": "",
                            "doctor": "",
                            "date": ""
                        },
                        "test_values": [ { "name": "", "value": "", "unit": "" } ],
                        "summary": ""
                    }

                    Here is the text:
                    """ + text);

            return ResponseEntity.ok(Map.of(
                    "ocr_text", text,
                    "ai_analysis", aiReply
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
