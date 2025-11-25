package com.aidoctor.controller;

import com.aidoctor.model.MedicalReport;
import com.aidoctor.service.PdfService;
import com.aidoctor.service.ReportProcessor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final ReportProcessor reportProcessor;
    private final PdfService pdfService;

    public OcrController(ReportProcessor reportProcessor, PdfService pdfService) {
        this.reportProcessor = reportProcessor;
        this.pdfService = pdfService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        try {
            String text = "";

            String contentType = file.getContentType();
            if (contentType != null && contentType.equals("application/pdf")) {
                try (InputStream in = file.getInputStream(); PDDocument doc = PDDocument.load(in)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    text = stripper.getText(doc);
                }
            } else {
                // attempt to read as image and do naive OCR-like fallback (just read alt text isn't available).
                // For proper OCR, integrate Tesseract. Here we read dimensions and put a placeholder.
                try (InputStream in = file.getInputStream()) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) {
                        text = "Image file detected (width=" + img.getWidth() + ", height=" + img.getHeight() + "). No OCR engine configured. To enable OCR, integrate Tesseract.";
                    } else {
                        text = "Unable to parse file contents.";
                    }
                }
            }

            // simple fields map empty (we didn't do field-based OCR)
            Map<String, String> fields = new HashMap<>();
            MedicalReport mr = reportProcessor.process(fields, text);

            // return summary + aiInterpretation + tests
            Map<String, Object> resp = new HashMap<>();
            resp.put("summary", mr.getSummary());
            resp.put("aiInterpretation", mr.getAiInterpretation());
            resp.put("docType", mr.getDocType());
            resp.put("tests", mr.getTests());
            resp.put("text", mr.getText());
            resp.put("pdfBytesPresent", mr.getPdfBytes() != null && mr.getPdfBytes().length > 0);

            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "OCR processing failed: " + ex.getMessage()));
        }
    }
}
