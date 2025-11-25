package com.aidoctor.service;

import com.aidoctor.model.MedicalReport;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrService {

    private final AiService aiService;

    public OcrService(AiService aiService) {
        this.aiService = aiService;
    }

    public MedicalReport process(MultipartFile file) throws Exception {
        // Dummy OCR: just return filename text
        String extractedText = "Extracted text from: " + file.getOriginalFilename();

        String summary = aiService.summarizeText(extractedText);

        MedicalReport report = new MedicalReport();
        report.setText(extractedText);
        report.setSummary(summary);

        return report;
    }
}
