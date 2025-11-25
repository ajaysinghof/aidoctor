package com.aidoctor.service;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.model.TestResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced ReportProcessor
 *
 * - preserves your original API and helper functions
 * - adds medical-term detection, numeric parsing, reference ranges, and doctor-style interpretation
 * - uses AiService for fallback corrections / interpretations
 */
@Service
public class ReportProcessor {

    private final DocumentClassifier classifier;
    private final AiService aiService;
    private final PdfService pdfService;

    public ReportProcessor(DocumentClassifier classifier, AiService aiService, PdfService pdfService) {
        this.classifier = classifier;
        this.aiService = aiService;
        this.pdfService = pdfService;
    }

    // crude "broken OCR" detector (unchanged)
    private boolean isOcrBroken(String text) {
        if (text == null) return true;
        String t = text.trim();
        if (t.length() < 40) return true; // too small
        // if many nonalpha scattered characters: mark broken (simple heuristic)
        int nonAlphaDigits = 0;
        for (char c : t.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && ",./:%()[]-".indexOf(c) < 0) nonAlphaDigits++;
        }
        if (((double) nonAlphaDigits / Math.max(1, t.length())) > 0.12) return true;
        return false;
    }

    // ---------------------------
    // NEW: Basic medical terms list (expandable)
    // ---------------------------
    private static final Set<String> MEDICAL_KEYWORDS = new HashSet<>(Arrays.asList(
            "hemoglobin", "hb", "wbc", "rbc", "platelet", "platelets", "rbc count",
            "cbc", "lft", "kft", "creatinine", "urea", "sgot", "sgpt", "alt", "ast", "alp",
            "bilirubin", "cholesterol", "hdl", "ldl", "triglycerides", "glucose",
            "hba1c", "thyroid", "tsh", "ft4", "ft3", "urine", "ph", "protein", "blood",
            "report", "laboratory", "lab", "test", "antibody", "culture", "sensitivity"
    ));

    // ---------------------------
    // NEW: Simple default reference ranges (male/female/units vary — keep basic defaults)
    // Expand this map as needed, or load from DB / config
    // ---------------------------
    private static final Map<String, double[]> DEFAULT_RANGES = new HashMap<>();
    private static final Map<String, String> DEFAULT_UNITS = new HashMap<>();
    static {
        // values: {low, high}
        DEFAULT_RANGES.put("hemoglobin", new double[]{13.5, 17.5}); // g/dL adult male (example)
        DEFAULT_RANGES.put("hb", new double[]{13.5, 17.5});
        DEFAULT_RANGES.put("wbc", new double[]{4.0, 11.0}); // x10^3/µL
        DEFAULT_RANGES.put("rbc", new double[]{4.5, 5.9}); // x10^6/µL male
        DEFAULT_RANGES.put("platelet", new double[]{150, 450}); // x10^3/µL
        DEFAULT_RANGES.put("platelets", new double[]{150, 450});
        DEFAULT_RANGES.put("creatinine", new double[]{0.7, 1.3}); // mg/dL (adult male)
        DEFAULT_RANGES.put("urea", new double[]{7, 20}); // mg/dL (example)
        DEFAULT_RANGES.put("glucose", new double[]{70, 110}); // fasting mg/dL
        DEFAULT_RANGES.put("hba1c", new double[]{4.0, 5.6}); // %
        DEFAULT_RANGES.put("cholesterol", new double[]{0, 200}); // mg/dL desirable <200
        DEFAULT_RANGES.put("hdl", new double[]{40, 60}); // mg/dL
        DEFAULT_RANGES.put("ldl", new double[]{0, 100}); // mg/dL
        // units (defaults)
        DEFAULT_UNITS.put("hemoglobin", "g/dL");
        DEFAULT_UNITS.put("hb", "g/dL");
        DEFAULT_UNITS.put("wbc", "10^3/uL");
        DEFAULT_UNITS.put("rbc", "10^6/uL");
        DEFAULT_UNITS.put("platelet", "10^3/uL");
        DEFAULT_UNITS.put("creatinine", "mg/dL");
        DEFAULT_UNITS.put("glucose", "mg/dL");
        DEFAULT_UNITS.put("hba1c", "%");
        DEFAULT_UNITS.put("cholesterol", "mg/dL");
    }

    // Regex to extract number and optional unit, e.g. "11.2 g/dL", "120mg/dL", "4.5"
    private static final Pattern VALUE_UNIT_PATTERN = Pattern.compile("([-+]?[0-9]{1,3}(?:[0-9,])*\\.?[0-9]*|\\d*\\.?\\d+)(?:\\s*([a-zA-Z%/\\^0-9]+))?");

    /**
     * MAIN PROCESS METHOD (keeps original structure and added features)
     */
    public MedicalReport process(Map<String, String> fields, String ocrText) {

        MedicalReport mr = new MedicalReport();
        String cleanedText = ocrText == null ? "" : ocrText;

        // 1) If OCR seems broken, ask AI to fix
        if (isOcrBroken(cleanedText)) {
            String corrected = aiService.fixBrokenMedicalText(cleanedText);
            if (corrected != null && !corrected.isBlank()) cleanedText = corrected;
        }

        // 2) classify using keywords
        DocumentClassifier.DocType type = classifier.classifyByKeywords(cleanedText);

        // 3) fallback to AI classification if unknown
        if (type == DocumentClassifier.DocType.UNKNOWN) {
            type = aiService.classifyDocumentWithAi(cleanedText);
        }

        // 4) If not a medical document (explicit NON_MEDICAL or UNKNOWN) -> return friendly error
        if (type == DocumentClassifier.DocType.NON_MEDICAL || type == DocumentClassifier.DocType.UNKNOWN) {
            mr.setSummary("❌ This does not appear to be a medical document. Please upload lab reports (CBC, LFT, KFT, Lipid Profile) or a prescription.");
            mr.setTests(new HashMap<>());
            mr.setDocType(type.name());
            return mr;
        }

        // 5) Build structured tests map from fields (your OCR fields map)
        Map<String, TestResult> testsMap = new LinkedHashMap<>(); // preserve order

        // Preserve the original parsing behavior but enrich TestResult with unit/ref-range/interpretation
        for (Map.Entry<String, String> e : fields.entrySet()) {
            String rawName = e.getKey().trim();
            String rawValue = e.getValue().trim();

            TestResult tr = new TestResult();
            tr.setName(rawName);
            tr.setRawValue(rawValue);

            // parse numeric and unit
            ParsedValue pv = parseNumericAndUnit(rawValue);
            if (pv != null && pv.hasNumber) {
                tr.setValue(pv.value);
                if (pv.unit != null) tr.setUnit(pv.unit);
            }

            // add default ref range if known
            double[] rng = lookupDefaultRangeFor(rawName);
            if (rng != null) {
                tr.setRefLow(rng[0]);
                tr.setRefHigh(rng[1]);
                // default unit if absent
                String du = DEFAULT_UNITS.getOrDefault(normalizeTestName(rawName), null);
                if (du != null && (tr.getUnit() == null || tr.getUnit().isBlank())) {
                    tr.setUnit(du);
                }
            }

            testsMap.put(rawName, tr);
        }

        // 6) If OCR fields empty, attempt to extract key-value pairs heuristically from cleanedText
        if (testsMap.isEmpty()) {
            // quick regex-based extraction: lines like "Hemoglobin: 11.2 g/dL" or "Hemoglobin 11.2"
            String[] lines = cleanedText.split("\\r?\\n");
            for (String line : lines) {
                // ignore short lines that are unlikely to be tests
                if (line.trim().length() < 6) continue;

                // try "Key : Value" first
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].trim();
                    String val = parts[1].trim();
                    TestResult tr = new TestResult();
                    tr.setName(key);
                    tr.setRawValue(val);
                    ParsedValue pv = parseNumericAndUnit(val);
                    if (pv != null && pv.hasNumber) {
                        tr.setValue(pv.value);
                        tr.setUnit(pv.unit);
                    }
                    double[] rng = lookupDefaultRangeFor(key);
                    if (rng != null) { tr.setRefLow(rng[0]); tr.setRefHigh(rng[1]); }
                    testsMap.put(key, tr);
                    continue;
                }

                // try "Key Value" patterns e.g. "Hemoglobin 11.2 g/dL"
                String[] parts = line.trim().split("\\s{2,}|\\t"); // large whitespace split
                if (parts.length >= 2) {
                    String key = parts[0].trim();
                    String val = parts[1].trim();
                    if (looksLikeValue(val)) {
                        TestResult tr = new TestResult();
                        tr.setName(key);
                        tr.setRawValue(val);
                        ParsedValue pv = parseNumericAndUnit(val);
                        if (pv != null && pv.hasNumber) {
                            tr.setValue(pv.value);
                            tr.setUnit(pv.unit);
                        }
                        double[] rng = lookupDefaultRangeFor(key);
                        if (rng != null) { tr.setRefLow(rng[0]); tr.setRefHigh(rng[1]); }
                        testsMap.put(key, tr);
                    }
                }
            }
        }

        // 7) Interpret each test (doctor-style). Use numeric ref ranges when available otherwise fallback to AI
        List<String> perTestInterpretations = new ArrayList<>();
        for (Map.Entry<String, TestResult> ent : testsMap.entrySet()) {
            TestResult tr = ent.getValue();
            String interp;

            if (tr.getValue() != null) {
                // numeric interpretation using ref ranges if provided, else using defaults
                double val = tr.getValue();
                Double low = tr.getRefLow();
                Double high = tr.getRefHigh();
                if (low == null || high == null) {
                    // attempt default lookup by normalized name
                    double[] def = lookupDefaultRangeFor(tr.getName());
                    if (def != null) { low = def[0]; high = def[1]; }
                }
                if (low != null && high != null) {
                    interp = interpretByRange(tr.getName(), val, low, high);
                    tr.setInterpretation(interp);
                } else {
                    // fallback to AI interpretation for numeric but no ranges available
                    interp = aiService.interpretNumericTest(tr.getName(), val, tr.getUnit());
                    if (interp == null || interp.isBlank()) {
                        interp = "Value: " + val + (tr.getUnit() != null ? " " + tr.getUnit() : "");
                    }
                    tr.setInterpretation(interp);
                }
            } else {
                // no numeric: ask AI to interpret the raw value
                String fallback = aiService.interpretRawTest(tr.getName(), tr.getRawValue());
                if (fallback == null || fallback.isBlank()) {
                    fallback = "Raw: " + tr.getRawValue();
                }
                tr.setInterpretation(fallback);
                interp = fallback;
            }

            // Build human-friendly per-test summary
            StringBuilder sb = new StringBuilder();
            sb.append(tr.getName()).append(": ");
            if (tr.getValue() != null) {
                sb.append(tr.getValue());
                if (tr.getUnit() != null && !tr.getUnit().isBlank()) sb.append(" ").append(tr.getUnit());
                sb.append(" — ").append(tr.getInterpretation());
            } else {
                sb.append(tr.getRawValue()).append(" — ").append(tr.getInterpretation());
            }
            perTestInterpretations.add(sb.toString());
        }

        // 8) Build doctor-style summary (short) and detailed summary
        String doctorSummary = buildDoctorSummary(perTestInterpretations);
        String detailedSummary = String.join("\n", perTestInterpretations);

        // 9) Ask AI for overall interpretation and recommendations (existing aiService call preserved)
        Map<String, String> simpleTests = new HashMap<>();
        testsMap.forEach((k, v) -> simpleTests.put(k, v.getRawValue()));

        // Enhance the inputs to AI: include cleanedText, simpleTests and doctorSummary (the aiService method may accept only two args,
        // but we keep original call and also call an extended method if available)
        String aiJson;
        try {
            // attempt to call extended method if present
            aiJson = aiService.summarizeAndInterpret(cleanedText, simpleTests, doctorSummary);
        } catch (NoSuchMethodError | AbstractMethodError ex) {
            // fallback to original method signature to preserve compatibility
            aiJson = aiService.summarizeAndInterpret(cleanedText, simpleTests);
            // if aiJson is plain or missing doctor's summary, we can append it locally
            if (aiJson == null || aiJson.isBlank()) {
                aiJson = "{\"summary\":\"" + doctorSummary + "\",\"details\":\"" + escapeForJson(detailedSummary) + "\"}";
            }
        } catch (Exception ex) {
            // if any unexpected error from AI service, still continue with local summary
            aiJson = "{\"summary\":\"" + doctorSummary + "\",\"details\":\"" + escapeForJson(detailedSummary) + "\"}";
        }

        // 10) Build JSON Response (preserve original fields)
        mr.setText(cleanedText);
        mr.setDocType(type.name());
        mr.setTests(simpleTests); // for existing API consumers (unchanged)
        mr.setAiInterpretation(aiJson);
        mr.setSummary(extractSummaryFromAiReply(aiJson)); // keep original helper

        // Append or ensure the doctorSummary appears in aiInterpretation (if not already)
        if (!aiJson.toLowerCase().contains("doctor") && !aiJson.toLowerCase().contains("summary")) {
            // naive append — not overwriting aiJson
            mr.setAiInterpretation(aiJson + "\n\n\"doctor_summary\": \"" + escapeForJson(doctorSummary) + "\"");
        }

        // 11) Also generate a clean PDF and attach raw bytes (or store S3 / DB)
        byte[] pdf = pdfService.generateReportPdf("AI Doctor - Corrected Report", cleanedText, simpleTests, mr.getAiInterpretation());
        mr.setPdfBytes(pdf);

        return mr;
    }

    // ---------------------------
    // HELPER: parse numeric and unit from a raw string
    // ---------------------------
    private static class ParsedValue {
        boolean hasNumber;
        double value;
        String unit;
    }

    private ParsedValue parseNumericAndUnit(String raw) {
        if (raw == null) return null;
        Matcher m = VALUE_UNIT_PATTERN.matcher(raw.replaceAll(",", "").trim());
        ParsedValue pv = new ParsedValue();
        if (m.find()) {
            String num = m.group(1);
            String unit = null;
            try {
                if (num != null && !num.isBlank()) {
                    pv.value = Double.parseDouble(num);
                    pv.hasNumber = true;
                } else {
                    pv.hasNumber = false;
                }
            } catch (Exception ex) {
                pv.hasNumber = false;
            }
            if (m.groupCount() >= 2) {
                unit = m.group(2);
                if (unit != null) unit = unit.trim();
            }
            pv.unit = unit;
            return pv;
        }
        return null;
    }

    // ---------------------------
    // HELPER: check whether token looks like a measurement / numeric snippet
    // ---------------------------
    private boolean looksLikeValue(String s) {
        if (s == null) return false;
        String cleaned = s.replaceAll(",", "").trim();
        return cleaned.matches(".*\\d+.*");
    }

    // ---------------------------
    // HELPER: normalize test name to lowercase simple token for matching defaults
    // ---------------------------
    private String normalizeTestName(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    // ---------------------------
    // HELPER: lookup default reference ranges by name (fuzzy)
    // ---------------------------
    private double[] lookupDefaultRangeFor(String name) {
        if (name == null) return null;
        String n = normalizeTestName(name);
        for (String key : DEFAULT_RANGES.keySet()) {
            String kNorm = key.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (n.contains(kNorm) || kNorm.contains(n)) {
                return DEFAULT_RANGES.get(key);
            }
        }
        return null;
    }

    // ---------------------------
    // HELPER: produce interpretation string given a numeric value and range
    // ---------------------------
    private String interpretByRange(String testName, double value, double low, double high) {
        // build friendly interpretation
        String name = testName == null ? "" : testName;
        if (Double.isNaN(value)) return "Unable to interpret";

        if (value < low) {
            double pct = (low - value) / Math.max(0.0001, low) * 100.0;
            return "Low (below reference " + low + " — consider clinical correlation)";
        } else if (value > high) {
            double pct = (value - high) / Math.max(0.0001, high) * 100.0;
            return "High (above reference " + high + " — consider follow-up)";
        } else {
            return "Within normal reference range (" + low + " - " + high + ")";
        }
    }

    // ---------------------------
    // HELPER: build short doctor summary from per-test lines
    // ---------------------------
    private String buildDoctorSummary(List<String> perTestLines) {
        if (perTestLines == null || perTestLines.isEmpty()) {
            return "No measurable test values detected. Please upload a lab report (CBC, LFT, KFT, Lipid profile) or provide clearer images.";
        }
        // group positives (abnormal) and normals
        List<String> abnormals = new ArrayList<>();
        List<String> normals = new ArrayList<>();
        for (String s : perTestLines) {
            String lower = s.toLowerCase();
            if (lower.contains("low") || lower.contains("high") || lower.contains("above") || lower.contains("below")) {
                abnormals.add(s);
            } else {
                normals.add(s);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Doctor Summary:\n");
        if (!abnormals.isEmpty()) {
            sb.append("Abnormal findings:\n");
            for (String a : abnormals) {
                sb.append(" - ").append(a).append("\n");
            }
        }
        if (!normals.isEmpty()) {
            sb.append("Within normal limits:\n");
            int limit = Math.min(6, normals.size());
            for (int i = 0; i < limit; i++) {
                sb.append(" - ").append(normals.get(i)).append("\n");
            }
            if (normals.size() > limit) {
                sb.append(" + ").append(normals.size() - limit).append(" more.\n");
            }
        }
        sb.append("\nRecommendations: correlate clinically, repeat abnormal tests if indicated, consult physician.");
        return sb.toString();
    }

    // crude parser to extract "summary" field from aiJson (which may be JSON or text)
    private String extractSummaryFromAiReply(String aiReply) {
        if (aiReply == null) return "";
        // try to find "summary" key in JSON-like reply
        int idx = aiReply.toLowerCase().indexOf("\"summary\"");
        if (idx >= 0) {
            // naive extraction
            int colon = aiReply.indexOf(":", idx);
            if (colon > 0) {
                int start = aiReply.indexOf("\"", colon);
                if (start > 0) {
                    int end = aiReply.indexOf("\"", start + 1);
                    if (end > start) return aiReply.substring(start + 1, end);
                }
            }
        }
        // fallback: return the first 300 chars
        return aiReply.length() > 300 ? aiReply.substring(0, 300) + "..." : aiReply;
    }

    // Small helper to escape JSON-ish values when we build fallback aiJson
    private String escapeForJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
