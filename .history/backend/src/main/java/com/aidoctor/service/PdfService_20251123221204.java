package com.aidoctor.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Very small PDF generator using OpenPDF (librepdf).
 */
@Service
public class PdfService {

    public byte[] generateReportPdf(String title, String cleanedText, Map<String,String> tests, String aiInterpretation) {
        try {
            Document doc = new Document(PageSize.A4, 36,36,36,36);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font h1 = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL);

            doc.add(new Paragraph(title, h1));
            doc.add(new Paragraph(" "));

            if (cleanedText != null && !cleanedText.isBlank()) {
                doc.add(new Paragraph("Original Text (cleaned):", new Font(Font.HELVETICA, 12, Font.BOLD)));
                doc.add(new Paragraph(cleanedText, normal));
                doc.add(new Paragraph(" "));
            }

            if (tests != null && !tests.isEmpty()) {
                doc.add(new Paragraph("Extracted Tests:", new Font(Font.HELVETICA, 12, Font.BOLD)));
                for (Map.Entry<String,String> e : tests.entrySet()) {
                    doc.add(new Paragraph(e.getKey() + " : " + e.getValue(), normal));
                }
                doc.add(new Paragraph(" "));
            }

            if (aiInterpretation != null && !aiInterpretation.isBlank()) {
                doc.add(new Paragraph("AI Interpretation:", new Font(Font.HELVETICA, 12, Font.BOLD)));
                doc.add(new Paragraph(aiInterpretation, normal));
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
}
