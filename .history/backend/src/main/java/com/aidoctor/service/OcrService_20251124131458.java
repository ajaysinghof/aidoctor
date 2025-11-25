package com.aidoctor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Minimal OCR service stub — returns very basic result so your upload flow doesn't crash.
 */
@Service
public class OcrService {

    public Map<String,String> process(MultipartFile file) throws IOException {
        // very simple stub: return filename and a small "extracted" text
        String fname = file == null ? "unknown" : file.getOriginalFilename();
        String text = "Extracted text stub for " + fname;
        return Map.of("fileName", fname, "text", text, "summary", "No summary (stub)");
    }

    // For old code expecting extractTextAsJson:
    public Map<String,Object> extractTextAsJson(MultipartFile file) throws IOException {
        return Map.of("fileName", file == null ? "none" : file.getOriginalFilename(), "summary", "stub summary", "text", "stub text");
    }
}
