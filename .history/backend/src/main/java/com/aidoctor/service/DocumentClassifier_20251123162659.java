package com.aidoctor.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DocumentClassifier {

    // Fast keyword based classifier - used first
    public enum DocType { LAB_REPORT, PRESCRIPTION, BILL, SCAN_REPORT, NON_MEDICAL, UNKNOWN }

    public DocType classifyByKeywords(String rawText) {
        if (rawText == null || rawText.isBlank()) return DocType.UNKNOWN;

        String t = rawText.toLowerCase(Locale.ROOT);

        // lab report keywords
        String[] labKeys = {"hemoglobin", "wbc", "rbc", "platelet", "cbc", "lft", "kft", "creatinine", "bilirubin", "glucose", "cholesterol", "hdl", "ldl", "triglyceride", "mg/dl"};
        for (String k : labKeys) if (t.contains(k)) return DocType.LAB_REPORT;

        // prescription
        String[] presKeys = {"prescription", "take", "tablet", "capsule", "mg", "bd", "od", "twice daily", "once daily", "apply"};
        for (String k : presKeys) if (t.contains(k)) return DocType.PRESCRIPTION;

        // bill/invoice
        String[] billKeys = {"total", "invoice", "amount", "bill", "gst", "tax", "rupees", "qty"};
        for (String k : billKeys) if (t.contains(k)) return DocType.BILL;

        // scan report
        String[] scanKeys = {"mri", "ct", "scan", "impression", "finding", "radiology"};
        for (String k : scanKeys) if (t.contains(k)) return DocType.SCAN_REPORT;

        return DocType.UNKNOWN;
    }
}
