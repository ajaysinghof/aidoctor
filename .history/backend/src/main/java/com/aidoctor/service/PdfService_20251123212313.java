package com.aidoctor.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class PdfService {

    public byte[] generateReportPdf(String title, String text, Map<String, String> tests, String aiInterpretation) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
            cs.setLeading(14.5f);
            cs.newLineAtOffset(40, 750);
            cs.showText(title);
            cs.newLine();

            cs.setFont(PDType1Font.HELVETICA, 10);
            cs.newLine();
            String[] lines = (text == null ? "" : text).split("\\r?\\n");
            int printed = 0;
            for (String l : lines) {
                if (printed++ > 35) {
                    cs.showText("... (truncated)"); cs.newLine(); break;
                }
                cs.showText(l.length() > 120 ? l.substring(0, 120) : l);
                cs.newLine();
            }
            cs.newLine();

            if (tests != null && !tests.isEmpty()) {
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.showText("---- Tests ----");
                cs.newLine();
                cs.setFont(PDType1Font.HELVETICA, 10);
                for (Map.Entry<String, String> e : tests.entrySet()) {
                    String line = e.getKey() + " : " + e.getValue();
                    cs.showText(line.length() > 120 ? line.substring(0, 120) : line);
                    cs.newLine();
                }
            }

            cs.newLine();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
            cs.showText("---- AI Interpretation ----");
            cs.newLine();
            cs.setFont(PDType1Font.HELVETICA, 10);
            String[] aiLines = (aiInterpretation == null ? "" : aiInterpretation).split("\\r?\\n");
            int aiPrinted = 0;
            for (String l : aiLines) {
                if (aiPrinted++ > 25) { cs.showText("..."); cs.newLine(); break; }
                cs.showText(l.length() > 120 ? l.substring(0, 120) : l);
                cs.newLine();
            }

            cs.endText();
            cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            return ("PDF generation error: " + e.getMessage()).getBytes();
        }
    }
}
