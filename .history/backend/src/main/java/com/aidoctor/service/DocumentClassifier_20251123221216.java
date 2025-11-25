package com.aidoctor.service;

import org.springframework.stereotype.Component;

@Component
public class DocumentClassifier {

    public enum DocType { MEDICAL, NON_MEDICAL, UNKNOWN }

    /**
     * Very simple keyword-based classifier. Add keywords as needed.
     */
    public DocType classifyByKeywords(String text) {
        if (text == null || text.isBlank()) return DocType.UNKNOWN;
        String t = text.toLowerCase();

        String[] medKeys = new String[] {
                "hemoglobin","wbc","rbc","platelet","cbc","lft","kft","blood","creatinine",
                "hemogram","glucose","cholesterol","triglyceride","hdl","ldl","urine","report",
                "mg/dl","g/dl","cells","u/l"
        };

        int hits = 0;
        for (String k : medKeys) {
            if (t.contains(k)) hits++;
        }

        if (hits >= 2) return DocType.MEDICAL;
        if (hits == 1) return DocType.UNKNOWN;
        return DocType.NON_MEDICAL;
    }
}
