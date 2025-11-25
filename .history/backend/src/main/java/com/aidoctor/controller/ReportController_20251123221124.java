package com.aidoctor.controller;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.service.OcrService;
import com.aidoctor.service.ReportProcessor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Exposes /api/report/analyze which streams "typing" then final JSON (as plain text).
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final OcrService ocrService;
    private final ReportProcessor processor;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public ReportController(OcrService ocrService, ReportProcessor processor) {
        this.ocrService = ocrService;
        this.processor = processor;
    }

    @PostMapping(path = "/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyze(@RequestParam("file") MultipartFile file) throws IOException {
        SseEmitter emitter = new SseEmitter(0L); // no timeout

        // immediately tell client that doctor is typing
        try {
            emitter.send("...typing");
        } catch (Exception ignored) {}

        // process async
        pool.submit(() -> {
            try {
                Map<String, Object> ocr = ocrService.extractTextAsJson(file);
                @SuppressWarnings("unchecked")
                Map<String,String> fields = (Map<String,String>) ocr.getOrDefault("fields", Map.of());
                String text = String.valueOf(ocr.getOrDefault("text",""));

                MedicalReport mr = processor.process(fields, text);

                // return final (as JSON string)
                String out = mr.getAiInterpretation();
                if (out == null || out.isBlank()) {
                    // build fallback JSON with summary + tests
                    out = "{ \"summary\": \"" + escape(mr.getSummary()) + "\", \"tests\": {} }";
                }

                emitter.send(out);
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send("{\"error\":\"" + escape(e.getMessage()) + "\"}");
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n","\\n").replace("\r","\\r");
    }
}
