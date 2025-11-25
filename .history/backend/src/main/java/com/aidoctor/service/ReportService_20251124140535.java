package com.aidoctor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * ReportService:
 *  - receives filename + extracted text
 *  - runs a quick medical / non-medical classifier (keywords)
 *  - if non-medical -> returns rejection with reason
 *  - if medical -> calls OpenAIService to summarize + produce a verification table
 *
 * Returned map format:
 *  {
 *    "fileName": "...",
 *    "text": "...",                // raw extracted text
 *    "isMedical": true/false,
 *    "reason": "...",              // present when isMedical==false
 *    "aiReply": "...",             // present when isMedical==true
 *    "summary": "..."              // short summary (first lines) if AI present
 *  }
 */
@Service
public class ReportService {

    private final OpenAIService openAIService;

    public ReportService(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    // crude medical keywords — simple heuristic, extend as needed
    private static final String[] MEDICAL_KEYWORDS = new String[]{
            "hemoglobin", "rbc", "wbc", "platelet", "cbc", "creatinine", "urea",
            "alt", "ast", "lft", "kft", "lipid", "cholesterol", "hdl", "ldl",
            "triglyceride", "blood sugar", "glucose", "hba1c", "prescription",
            "mg/dl", "g/dl", "report", "radiology", "x-ray", "ultrasound", "ct scan", "mri"
    };

    private boolean looksMedical(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        int matches = 0;
        for (String k : MEDICAL_KEYWORDS) {
            if (lower.contains(k)) matches++;
        }
        // require at least one keyword; but if length short, be stricter
        return matches >= 1;
    }

    /**
     * Main method called by controller.
     */
    public Map<String, Object> processAndInterpret(String filename, String extractedText) {

        Map<String, Object> out = new HashMap<>();
        out.put("fileName", filename == null ? "unknown" : filename);
        out.put("text", extractedText == null ? "" : extractedText);

        // 1) Quick local medical check
        boolean isMedical = looksMedical(extractedText);

        // If local heuristic fails, we can ask the AI to classify; but avoid calling AI for every file unnecessarily.
        if (!isMedical) {
            // Ask AI to check whether the text looks medical (short prompt)
            String system = "You are an assistant that classifies whether a document is a medical document (lab report, prescription, diagnostic report). Respond with: MEDICAL or NON_MEDICAL followed by a short reason.";
            String userPrompt = "Classify this extracted text (return MEDICAL or NON_MEDICAL and one-line reason):\n\n" + (extractedText == null ? "" : extractedText.substring(0, Math.min(1500, extractedText.length())));
            String aiClassify = openAIService.askOpenAI(system, userPrompt);

            // Simple parse: if aiClassify contains MEDICAL
            if (aiClassify != null && aiClassify.toUpperCase().contains("MEDICAL")) {
                isMedical = true;
            } else {
                // treat as non-medical; set reason to AI response (or local message)
                String reason = "Local heuristic: no medical keywords found.";
                if (aiClassify != null && !aiClassify.isBlank()) {
                    reason = "AI: " + aiClassify;
                }
                out.put("isMedical", false);
                out.put("reason", reason);
                return out;
            }
        }

        // 2) If medical -> compose prompt and call OpenAI to summarize & produce verification table
        out.put("isMedical", true);

        String systemPrompt = "You are a senior physician and medical data assistant. Given extracted raw text from a medical document (lab report or prescription), produce:\n" +
                "1) A short patient-facing summary (2-4 sentences).\n" +
                "2) A verification table listing probable patient name (if found), document type (prescription / lab report / diagnostic), and a short list of key tests found with values where possible.\n" +
                "Return a JSON object with keys: summary, docType, patientName, tests (map testName->value), notes.\n" +
                "If you are unsure about any field put null or an empty map.";

        String userPrompt = "Extracted Text:\n" + (extractedText == null ? "" : extractedText);

        String aiReply = openAIService.askOpenAI(systemPrompt, userPrompt);

        // Return AI reply and also an easy summary string
        out.put("aiReply", aiReply == null ? "AI returned no reply" : aiReply);

        // produce short 'summary' field: use first 300 chars of aiReply or AI-provided summary key if present
        String summary = extractSummaryFromAiReply(aiReply);
        out.put("summary", summary);

        return out;
    }

    // naive extractor to pick summary key from AI JSON reply if present, else fallback
    private String extractSummaryFromAiReply(String aiReply) {
        if (aiReply == null) return "No summary available.";
        String lower = aiReply.toLowerCase();
        // look for "summary" token
        int idx = lower.indexOf("\"summary\"");
        if (idx >= 0) {
            int colon = aiReply.indexOf(":", idx);
            if (colon > 0) {
                int start = aiReply.indexOf('"', colon);
                if (start > 0) {
                    int end = aiReply.indexOf('"', start + 1);
                    if (end > start) return aiReply.substring(start + 1, end);
                }
            }
        }
        // If AI returned plain text, take first 300 chars
        String trimmed = aiReply.trim();
        if (trimmed.isBlank()) return "No summary available.";
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }
}
