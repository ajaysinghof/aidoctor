package com.aidoctor.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.StringJoiner;

/**
 * AiService communicates with OpenAiClient and provides:
 * - greeting flow
 * - doctor chat
 * - OCR correction
 * - document classification
 * - medical interpretation
 * - structured summarization (JSON)
 */
@Service
public class AiService {

    private final OpenAiClient openAiClient;

    public AiService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    // -----------------------------------------------------------
    // 1️⃣ INITIAL GREETING — ("Hello" → show options)
    // -----------------------------------------------------------
    public String initialGreeting(String userInput) {
        String prompt = """
            You are Dr. Raghav, an AI Medical Assistant.

            The patient has just started the chat. 
            Greet them warmly and offer two clear options:

            1) "Chat with Doctor"
            2) "Upload Report for Analysis"

            If the patient’s name is mentioned, include it naturally.
            Keep it short and friendly.

            Patient message: "%s"
        """.formatted(userInput);

        return openAiClient.simpleChat(prompt);
    }

    // -----------------------------------------------------------
    // 2️⃣ ONGOING DOCTOR CHAT ("Doctor is typing…")
    // -----------------------------------------------------------
    public String chatWithDoctor(String userMessage) {
        String prompt = """
            You are Dr. Raghav, a senior medical specialist.

            Continue the consultation based on this patient message.
            Be concise, empathetic, medically accurate.
            Do NOT provide long paragraphs—keep answers readable.

            Patient message:
            %s
        """.formatted(userMessage);

        return openAiClient.simpleChat(prompt);
    }

    // -----------------------------------------------------------
    // 3️⃣ FIX BROKEN OCR TEXT
    // -----------------------------------------------------------
    public String fixBrokenMedicalText(String brokenText) {
        if (brokenText == null || brokenText.isBlank()) return brokenText;

        String prompt = """
            You are a clinical text corrector.

            The following text is from OCR extraction and may be broken.
            Carefully clean it while preserving:
            - medical terms
            - test names
            - values
            - units

            Return ONLY corrected text. No explanation.

            Broken OCR text:
            %s
        """.formatted(brokenText);

        return openAiClient.simpleChat(prompt);
    }

    // -----------------------------------------------------------
    // 4️⃣ AI-BASED CLASSIFICATION
    // -----------------------------------------------------------
    public DocumentClassifier.DocType classifyDocumentWithAi(String text) {
        if (text == null || text.isBlank())
            return DocumentClassifier.DocType.UNKNOWN;

        String prompt = """
            Classify the following document strictly into one word:
            
            MEDICAL
            NON_MEDICAL
            UNKNOWN

            Only return the single word.

            Document text:
            %s
        """.formatted(text);

        String resp = openAiClient.simpleChat(prompt).trim().toUpperCase();

        try {
            return DocumentClassifier.DocType.valueOf(resp);
        } catch (Exception e) {
            return DocumentClassifier.DocType.UNKNOWN;
        }
    }

    // -----------------------------------------------------------
    // 5️⃣ SUMMARIZE + INTERPRET (JSON OUTPUT)
    // -----------------------------------------------------------
    public String summarizeAndInterpret(String cleanedText, Map<String, String> tests) {
        StringJoiner sj = new StringJoiner("\n");

        sj.add("""
            You are a medical AI. Analyze the text AND the extracted test values.
            Output must be STRICT JSON with these fields ONLY:

            {
              "summary": "...",
              "findings": ["...", "..."],
              "recommendations": ["...", "..."]
            }

            Rules:
            - Keep summary short (1–2 sentences max)
            - Findings: lab deviations, suspected issues
            - Recommendations: what the patient should do (general guidance)
            - STRICT JSON only — no markdown, no comments
        """);

        sj.add("\nDocument Text:\n" + cleanedText);
        sj.add("\nExtracted Tests:");

        for (Map.Entry<String, String> e : tests.entrySet()) {
            sj.add(e.getKey() + ": " + e.getValue());
        }

        return openAiClient.simpleChat(sj.toString());
    }

    // -----------------------------------------------------------
    // 6️⃣ INTERPRET A SINGLE NUMERIC TEST VALUE
    // -----------------------------------------------------------
    public String interpretNumericTest(String name, double value, String unit) {
        String prompt = """
            Interpret this medical test result in plain clinician-style language.

            Test: %s
            Value: %s %s

            Reply in ONE short sentence like:
            - "Hemoglobin is mildly low."
            - "WBC count is within normal range."
        """.formatted(name, value, unit == null ? "" : unit);

        return openAiClient.simpleChat(prompt);
    }

    // -----------------------------------------------------------
    // 7️⃣ INTERPRET UNSTRUCTURED RAW TEST VALUE
    // -----------------------------------------------------------
    public String interpretRawTest(String name, String rawValue) {
        String prompt = """
            Interpret this raw medical test value:

            Test Name: %s
            Raw Value: %s

            Provide a short clinician-style interpretation.
        """.formatted(name, rawValue);

        return openAiClient.simpleChat(prompt);
    }

}
