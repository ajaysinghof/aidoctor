package com.aidoctor.model;

import java.util.HashMap;
import java.util.Map;

public class MedicalReport {
    private String text;
    private String docType;
    private Map<String, TestResult> tests = new HashMap<>();
    private String aiInterpretation;
    private String summary;
    private byte[] pdfBytes;

    public MedicalReport() {}

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public Map<String, TestResult> getTests() { return tests; }
    public void setTests(Map<String, TestResult> tests) { this.tests = tests; }

    public String getAiInterpretation() { return aiInterpretation; }
    public void setAiInterpretation(String aiInterpretation) { this.aiInterpretation = aiInterpretation; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public byte[] getPdfBytes() { return pdfBytes; }
    public void setPdfBytes(byte[] pdfBytes) { this.pdfBytes = pdfBytes; }
}
