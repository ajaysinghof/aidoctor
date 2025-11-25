package com.aidoctor.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiService {

    private final ChatService chatService; // your existing low-level OpenAI caller
    private final DocumentClassifier classifier;

    public AiService(ChatService chatService, DocumentClassifier classifier) {
        this.chatService = chatService;
        this.classifier = classifier;
    }

    // 1) Use ChatGPT to correct broken OCR medical text
    public String fixBrokenMedicalText(String rawText) {
        if (rawText == null || rawText.isBlank()) return rawText;
        String prompt = """
            You are an expert medical report assistant. The following text is OCR output from a lab report and may contain spelling mistakes or broken words. 
            1) Correct obvious OCR errors and misspellings.
            2) Expand common abbreviations (e.g., Hgb -> Hemoglobin, WBC -> White Blood Cells).
            3) Preserve numeric values and units; do not invent values.
            4) Return only the cleaned text (no commentary).
            
            OCR text:
            %s
            """.formatted(rawText.replace("\"","'"));

        return chatService.ask(prompt).trim();
    }

    // 2) Ask ChatGPT to summarize & interpret lab tests, doctor-style:
    //    testsMap is optional structured data; you can include it in prompt or let model read text.
    public String summarizeAndInterpret(String cleanedText, Map<String, String> testsMap) {
        String testsPart = "";
        if (testsMap != null && !testsMap.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            testsMap.forEach((k,v) -> sb.append(k).append(": ").append(v).append("\n"));
            testsPart = sb.toString();
        }

        String prompt = """
            You are Dr. Raghav, a senior general physician. Read the medical report below and produce:
            1) A short medical summary (2-4 sentences) for the patient in plain language.
            2) A list of key test interpretations (e.g., "Hemoglobin 10.5 g/dL: low — mild anemia").
            3) Any urgent flags that require immediate attention.
            4) Suggested next steps (labs/doctor follow-up), concise.

            Provide JSON output with fields: "summary", "interpretations" (array), "flags" (array), "recommendations" (array).

            Report / tests:
            %s

            Structured tests:
            %s
            """.formatted(cleanedText.replace("\"","'"), testsPart);

        String reply = chatService.ask(prompt);
        return reply;
    }

    // 3) Deep classification using AI (fallback)
    public DocumentClassifier.DocType classifyDocumentWithAi(String rawText) {
        String prompt = """
            You are a document classifier. Classify the following text as one of: LAB_REPORT, PRESCRIPTION, BILL, SCAN_REPORT, NON_MEDICAL.
            Reply with only the single label.
            
            Text:
            %s
            """.formatted(rawText.replace("\"","'"));

        String reply = chatService.ask(prompt).trim().toUpperCase();
        try {
            return DocumentClassifier.DocType.valueOf(reply.split("\\s+")[0]);
        } catch (Exception ex) {
            return DocumentClassifier.DocType.UNKNOWN;
        }
    }
}
