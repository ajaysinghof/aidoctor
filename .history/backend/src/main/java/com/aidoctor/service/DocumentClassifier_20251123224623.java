package com.aidoctor.service;

import org.springframework.stereotype.Component;

@Component
public class DocumentClassifier {

    public enum DocType { MEDICAL, NON_MEDICAL, UNKNOWN }

    public DocType classifyByKeywords(String text) {
        if (text == null || text.isBlank()) return DocType.UNKNOWN;
        String t = text.toLowerCase();
        if (t.contains("hemoglobin") || t.contains("wbc") || t.contains("cbc") || t.contains("lft") || t.contains("glucose") || t.contains("platelet"))
            return DocType.MEDICAL;
        if (t.length() < 50) return DocType.UNKNOWN;
        // non-medical hints
        if (t.contains("invoice") || t.contains("thank you") || t.contains("address") || t.contains("job")) return DocType.NON_MEDICAL;
        return DocType.UNKNOWN;
    }
}
