package com.aidoctor.service;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.model.TestResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReportProcessor {

    // Map keywords -> canonical test name and default reference ranges (adult, general).
    // You can extend ranges by sex/age later.
    private static final List<TestDefinition> TEST_DEFINITIONS = List.of(
            new TestDefinition("hemoglobin", "Hemoglobin", "g/dL", 13.5, 17.5), // male typical (we'll treat as general)
            new TestDefinition("hb", "Hemoglobin", "g/dL", 13.5, 17.5),
            new TestDefinition("wbc", "White Blood Cells", "10^3/µL", 4.0, 11.0),
            new TestDefinition("rbc", "Red Blood Cells", "10^6/µL", 4.5, 5.9),
            new TestDefinition("platelet", "Platelet Count", "10^3/µL", 150.0, 450.0),
            new TestDefinition("platelets", "Platelet Count", "10^3/µL", 150.0, 450.0),
            new TestDefinition("neutrophil", "Neutrophils", "%", 40.0, 70.0),
            new TestDefinition("lymphocyte", "Lymphocytes", "%", 20.0, 40.0),
            new TestDefinition("esr", "ESR", "mm/hr", 0.0, 20.0),
            new TestDefinition("glucose (fasting)", "Glucose (Fasting)", "mg/dL", 70.0, 100.0),
            new TestDefinition("creatinine", "Creatinine", "mg/dL", 0.6, 1.3),
            new TestDefinition("urea", "Urea (BUN)", "mg/dL", 7.0, 20.0),
            new TestDefinition("bilirubin", "Bilirubin (Total)", "mg/dL", 0.1, 1.2)
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)");
    // pattern for explicit ranges like (11.0 - 15.0) or 11.0-15.0 or 11–15
    private static final Pattern RANGE_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*[\\-–]\\s*([0-9]+(?:\\.[0-9]+)?)");

    // helper class
    private static class TestDefinition {
        String keyword;
        String canonicalName;
        String unit;
        Double low;
        Double high;
        TestDefinition(String keyword, String canonicalName, String unit, Double low, Double high) {
            this.keyword = keyword;
            this.canonicalName = canonicalName;
            this.unit = unit;
            this.low = low;
            this.high = high;
        }
    }

    /**
     * Main entry: accepts OCR key-value map (key->value) and optional full text,
     * returns a MedicalReport with parsed tests and a doctor-style summary.
     */
    public MedicalReport process(Map<String, String> ocrFields, String fullText) {
        Map<String, TestResult> tests = new LinkedHashMap<>();

        // Normalize keys and try to match known tests
        for (Map.Entry<String, String> e : ocrFields.entrySet()) {
            String rawKey = e.getKey();
            String rawValue = e.getValue();
            if (rawValue == null || rawValue.isBlank()) continue;

            String keyLower = rawKey.toLowerCase().trim();

            // attempt to find a test definition by keyword contained in key
            Optional<TestDefinition> defOpt = findDefinitionForKey(keyLower);

            if (defOpt.isPresent()) {
                TestDefinition def = defOpt.get();

                // parse numeric value from rawValue (take first number or the number before unit)
                ParsedValue pv = parseValueAndRange(rawValue);

                Double value = pv.value;
                String unit = pv.unit != null ? pv.unit : def.unit;
                Double refLow = def.low;
                Double refHigh = def.high;

                // if OCR gave explicit range override defaults
                if (pv.rangeLow != null && pv.rangeHigh != null) {
                    refLow = pv.rangeLow;
                    refHigh = pv.rangeHigh;
                }

                String interpretation = interpretValue(value, refLow, refHigh);

                TestResult tr = new TestResult();
                tr.setName(def.canonicalName);
                tr.setValue(value);
                tr.setUnit(unit);
                tr.setRefLow(refLow);
                tr.setRefHigh(refHigh);
                tr.setInterpretation(interpretation);
                tr.setRawValue(rawValue);

                tests.put(def.canonicalName, tr);
            } else {
                // attempt to parse unknown test with number and keep raw key
                ParsedValue pv = parseValueAndRange(rawValue);
                if (pv.value != null) {
                    String guessedName = normalizeKeyToName(rawKey);
                    TestResult tr = new TestResult();
                    tr.setName(guessedName);
                    tr.setValue(pv.value);
                    tr.setUnit(pv.unit);
                    tr.setRefLow(null);
                    tr.setRefHigh(null);
                    tr.setInterpretation("unknown");
                    tr.setRawValue(rawValue);
                    tests.put(guessedName, tr);
                }
            }
        }

        // Build summary
        String summary = buildSummary(tests);

        MedicalReport report = new MedicalReport();
        report.setText(fullText);
        report.setTests(tests);
        report.setSummary(summary);
        return report;
    }

    // find definition by checking if keyword token exists in key text
    private Optional<TestDefinition> findDefinitionForKey(String keyLower) {
        for (TestDefinition def : TEST_DEFINITIONS) {
            if (keyLower.contains(def.keyword)) return Optional.of(def);
        }
        return Optional.empty();
    }

    // Convert weird OCR keys to readable name
    private String normalizeKeyToName(String rawKey) {
        return rawKey.trim().replaceAll("[\\s:_\\-]+", " ");
    }

    // parse numeric and optional range and try to extract unit if present (like g/dL)
    private static class ParsedValue {
        Double value;
        String unit;
        Double rangeLow;
        Double rangeHigh;
    }

    private ParsedValue parseValueAndRange(String text) {
        ParsedValue p = new ParsedValue();
        if (text == null) return p;
        String t = text.replaceAll(",", "."); // commas -> dots for decimals
        // first look for explicit range
        Matcher rangeMatcher = RANGE_PATTERN.matcher(t);
        if (rangeMatcher.find()) {
            try {
                p.rangeLow = Double.parseDouble(rangeMatcher.group(1));
                p.rangeHigh = Double.parseDouble(rangeMatcher.group(2));
            } catch (Exception ignored) {}
        }

        // first number usually the measured value
        Matcher m = NUMBER_PATTERN.matcher(t);
        if (m.find()) {
            try {
                p.value = Double.parseDouble(m.group(1));
            } catch (Exception ignored) {}
        }

        // attempt to capture unit (letters and / % symbols) after the number
        // e.g. "12.5 g/dL", "150 x10^3/uL", "7 mg/dL"
        Pattern unitPattern = Pattern.compile("(?:[0-9]+(?:\\.[0-9]+)?)[^0-9\\.]*([a-zA-Z%/\\^\\d\\u00B3\\u00B2\\*]+)");
        Matcher um = unitPattern.matcher(t);
        if (um.find()) {
            String unitCandidate = um.group(1).trim();
            // very often unitCandidate may include extra chars; normalize few common tokens
            unitCandidate = unitCandidate.replaceAll("\\s+", " ");
            p.unit = unitCandidate;
        }

        return p;
    }

    private String interpretValue(Double value, Double low, Double high) {
        if (value == null) return "invalid";
        if (low == null || high == null) return "unknown";
        if (value < low) return "low";
        if (value > high) return "high";
        return "normal";
    }

    // Build a simple doctor-style summary; can be extended to be more natural
    private String buildSummary(Map<String, TestResult> tests) {
        if (tests == null || tests.isEmpty()) return "No numeric test results found.";

        StringBuilder sb = new StringBuilder();
        List<String> lows = new ArrayList<>();
        List<String> highs = new ArrayList<>();
        List<String> normals = new ArrayList<>();
        List<String> unknowns = new ArrayList<>();

        for (TestResult tr : tests.values()) {
            String name = tr.getName();
            Double val = tr.getValue();
            String unit = tr.getUnit() != null ? " " + tr.getUnit() : "";
            String interp = tr.getInterpretation();

            String snippet;
            if (val != null) snippet = String.format("%s: %.2f%s", name, val, unit);
            else snippet = String.format("%s: %s", name, tr.getRawValue());

            switch (interp) {
                case "low" -> lows.add(snippet);
                case "high" -> highs.add(snippet);
                case "normal" -> normals.add(snippet);
                default -> unknowns.add(snippet);
            }
        }

        if (!lows.isEmpty()) {
            sb.append("Low: ");
            sb.append(String.join("; ", lows));
            sb.append(". ");
        }
        if (!highs.isEmpty()) {
            sb.append("High: ");
            sb.append(String.join("; ", highs));
            sb.append(". ");
        }
        if (!normals.isEmpty()) {
            sb.append("Within normal range: ");
            sb.append(String.join("; ", normals));
            sb.append(". ");
        }
        if (!unknowns.isEmpty()) {
            sb.append("Requires review: ");
            sb.append(String.join("; ", unknowns));
            sb.append(". ");
        }

        return sb.toString().trim();
    }
}
