package com.aidoctor.model;

public class TestResult {

    private String name;
    private Double value;
    private String unit;
    private Double refLow;
    private Double refHigh;
    private String interpretation;
    private String rawValue;

    public TestResult() {}

    public TestResult(String name, Double value, String unit,
                      Double refLow, Double refHigh, String interpretation, String rawValue) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.refLow = refLow;
        this.refHigh = refHigh;
        this.interpretation = interpretation;
        this.rawValue = rawValue;
    }

    // -------------------
    // GETTERS AND SETTERS
    // -------------------

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getRefLow() { return refLow; }
    public void setRefLow(Double refLow) { this.refLow = refLow; }

    public Double getRefHigh() { return refHigh; }
    public void setRefHigh(Double refHigh) { this.refHigh = refHigh; }

    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }

    public String getRawValue() { return rawValue; }
    public void setRawValue(String rawValue) { this.rawValue = rawValue; }
}
