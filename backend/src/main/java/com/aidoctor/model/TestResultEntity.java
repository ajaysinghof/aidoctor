package com.aidoctor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "test_results")
public class TestResultEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double value;
    private String unit;
    private Double refLow;
    private Double refHigh;
    private String interpretation;

    @Lob
    private String rawValue;

    @ManyToOne(fetch = FetchType.LAZY)
    private Report report;

    // getters & setters
    public Long getId(){return id;}
    public void setId(Long id){this.id = id;}
    public String getName(){return name;}
    public void setName(String name){this.name = name;}
    public Double getValue(){return value;}
    public void setValue(Double value){this.value = value;}
    public String getUnit(){return unit;}
    public void setUnit(String unit){this.unit = unit;}
    public Double getRefLow(){return refLow;}
    public void setRefLow(Double refLow){this.refLow = refLow;}
    public Double getRefHigh(){return refHigh;}
    public void setRefHigh(Double refHigh){this.refHigh = refHigh;}
    public String getInterpretation(){return interpretation;}
    public void setInterpretation(String interpretation){this.interpretation = interpretation;}
    public String getRawValue(){return rawValue;}
    public void setRawValue(String rawValue){this.rawValue = rawValue;}
    public Report getReport(){return report;}
    public void setReport(Report report){this.report = report;}
}
