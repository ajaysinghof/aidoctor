package com.aidoctor.controller;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.service.ReportProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportProcessor processor;

    @Autowired
    public ReportController(ReportProcessor processor) {
        this.processor = processor;
    }

    /**
     * POST /api/reports/process
     *
     * Accepts JSON:
     * {
     *   "fields": {
     *      "Hemoglobin": "12.5 g/dL",
     *      "WBC": "5400 /uL"
     *   },
     *   "text": "full OCR text"
     * }
     */
    @PostMapping("/process")
    public MedicalReport processReport(@RequestBody Map<String, Object> body) {

        Map<String, String> fields;
        String fullText = null;

        // If the body contains "fields", use it.
        if (body.containsKey("fields")) {
            Object f = body.get("fields");
            //noinspection unchecked
            fields = (Map<String, String>) f;

            if (body.containsKey("text"))
                fullText = String.valueOf(body.get("text"));

        } else {
            // If only flat fields are sent, convert values to string
            fields = body.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.valueOf(e.getValue())
                    ));
        }

        return processor.process(fields, fullText);
    }
}
