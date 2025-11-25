package com.aidoctor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class ReportService {

    private final OcrService ocrService;
    private final ReportProcessor processor;

    public ReportService(OcrService ocrService, ReportProcessor processor) {
        this.ocrService = ocrService;
        this.processor = processor;
    }

    public Map<String, Object> processReport(MultipartFile file) {
        try {
            // 1. Extract text from AWS Textract
            Map<String, Object> ocrJson = ocrService.extractTextAsJson(file);

            String text = ocrJson.get("text").toString();

            // 2. AI processing (classification, summary, interpretation)
            Map<String, Object> finalReport = processor.process(text);

            // 3. Merge OCR + AI result
            finalReport.put("ocrText", text);

            return finalReport;

        } catch (Exception ex) {
            return Map.of("error", "Report processing failed: " + ex.getMessage());
        }
    }
}
