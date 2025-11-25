package com.aidoctor.controller;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.service.OcrService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin
public class OcrController {

    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping("/extract")
    public MedicalReport extract(@RequestParam("file") MultipartFile file) throws Exception {
        return ocrService.process(file);
    }
}
