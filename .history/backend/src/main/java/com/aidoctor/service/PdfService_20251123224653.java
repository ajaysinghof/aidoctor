package com.aidoctor.service;

import com.github.librepdf.openpdf.text.Document;
import com.github.librepdf.openpdf.text.Paragraph;
import com.github.librepdf.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class PdfService {

    public byte[] generateReportPdf(String title, String text, Map<String, String> tests, String interpretation) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            doc.add(new Paragraph(title));
            doc.add(new Paragraph("\nSummary / Interpretation:\n" + (interpretation == null ? "" : interpretation)));
            doc.add(new Paragraph("\n--- Extracted Text ---\n" + (text == null ? "" : text)));
            if (tests != null && !tests.isEmpty()) {
                doc.add(new Paragraph("\n--- Tests ---"));
                for (Map.Entry<String, String> e : tests.entrySet()) {
                    doc.add(new Paragraph(e.getKey() + " : " + e.getValue()));
                }
            }
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
