package com.aidoctor.service;

import org.springframework.stereotype.Component;

@Component
public class DocumentClassifier {

    public enum DocType {
        MEDICAL,
        NON_MEDICAL,
        UNKNOWN
    }

    /**
     * Basic keyword-based classification. Expand keywords as needed.
     */
    public DocType classifyByKeywords(String text) {
        if (text == null || text.isBlank()) return DocType.UNKNOWN;
        String lower = text.toLowerCase();

        // quick indicators of medical lab report
        String[] medicalKeywords = new String[]{
                "hemoglobin", "wbc", "rbc", "platelet", "liver", "alt", "ast", "bilirubin",
                "creatinine", "urea", "cholesterol", "triglyceride", "hdl", "ldl", "cbc",
                "hemogram", "glucose", "hba1c", "urine", "urinalysis", "mg/dl", "g/dl", "cells/μl"
        };

        int score = 0;
        for (String k : medicalKeywords) {
            if (lower.contains(k)) score++;
            if (score >= 2) return DocType.MEDICAL;
        }

        // explicit non-medical hints
        String[] nonMedical = new String[]{"invoice", "receipt", "bank", "statement", "resume", "cv"};
        for (String k : nonMedical) if (lower.contains(k)) return DocType.NON_MEDICAL;

        return DocType.UNKNOWN;
    }
}
