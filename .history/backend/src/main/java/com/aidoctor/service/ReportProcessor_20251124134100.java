package com.aidoctor.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportProcessor {

    private final AiService aiService;

    public ReportProcessor(AiService aiService) {
        this.aiService = aiService;
    }

    public Map<String, Object> process(String text) {

        Map<String, Object> result = new HashMap<>();

        if (text == null || text.isBlank()) {
            return Map.of("error", "OCR produced no text.");
        }

        // 1. Classify document type
        String docType = aiService.classifyDocumentWithAi(text);

        // 2. AI interpretation (summary, findings, recommendations)
        String aiJson = aiService.summarizeAndInterpret(text, Map.of());

        result.put("docType", docType);
        result.put("aiInterpretation", aiJson);

        // Try extracting a summary
        result.put("summary", aiService.simpleChat("Summarize briefly:\n" + text));

        return result;
    }
}
