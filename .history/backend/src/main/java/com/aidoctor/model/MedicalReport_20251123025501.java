package com.aidoctor.model;

import java.util.Map;

public class MedicalReport {

    private String status = "success";
    private String text;
    private Map<String, TestResult> tests;
    private String summary;

    public MedicalReport() {}

    public MedicalReport(String text, Map<String, TestResult> tests, String summary) {
        this.text = text;
        this.tests = tests;
        this.summary = summary;
    }

    // -------------------
    // GETTERS AND SETTERS
    // -------------------

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Map<String, TestResult> getTests() { return tests; }
    public void setTests(Map<String, TestResult> tests) { this.tests = tests; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
