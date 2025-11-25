package com.aidoctor.model;

import java.util.Map;

public class MedicalReport {

    private String text;
    private String summary;
    private String docType;

    // SIMPLE tests for API consumers → Map<String, String>
    private Map<String, String> tests;

    // AI interpretation JSON or text
    private String aiInterpretation;

    // PDF bytes (optional)
    private byte[] pdfBytes;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public Map<String, String> getTests() {
        return tests;
    }

    public void setTests(Map<String, String> tests) {
        this.tests = tests;
    }

    public String getAiInterpretation() {
        return aiInterpretation;
    }

    public void setAiInterpretation(String aiInterpretation) {
        this.aiInterpretation = aiInterpretation;
    }

    public byte[] getPdfBytes() {
        return pdfBytes;
    }

    public void setPdfBytes(byte[] pdfBytes) {
        this.pdfBytes = pdfBytes;
    }
}
