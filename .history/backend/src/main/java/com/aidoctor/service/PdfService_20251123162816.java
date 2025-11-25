package com.aidoctor.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class PdfService {

    public byte[] generateReportPdf(String title, String cleanedText, Map<String, String> tests, String aiSummary) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font h1 = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font mono = new Font(Font.COURIER, 10, Font.NORMAL);

            Paragraph header = new Paragraph(title, h1);
            header.setAlignment(Element.ALIGN_CENTER);
            doc.add(header);
            doc.add(Chunk.NEWLINE);

            Paragraph s = new Paragraph("AI-corrected OCR text:", new Font(Font.HELVETICA, 12, Font.BOLD));
            doc.add(s);
            doc.add(new Paragraph(cleanedText, mono));
            doc.add(Chunk.NEWLINE);

            if (tests != null && !tests.isEmpty()) {
                Paragraph testsP = new Paragraph("Extracted Tests:", new Font(Font.HELVETICA, 12, Font.BOLD));
                doc.add(testsP);

                for (Map.Entry<String, String> e : tests.entrySet()) {
                    doc.add(new Paragraph(e.getKey() + ": " + e.getValue(), normal));
                }
                doc.add(Chunk.NEWLINE);
            }

            if (aiSummary != null && !aiSummary.isBlank()) {
                Paragraph aiP = new Paragraph("Doctor Summary (AI):", new Font(Font.HELVETICA, 12, Font.BOLD));
                doc.add(aiP);
                doc.add(new Paragraph(aiSummary, normal));
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("PDF generation failed: " + ex.getMessage(), ex);
        }
    }
}
