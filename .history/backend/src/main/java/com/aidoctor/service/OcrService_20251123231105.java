package com.aidoctor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import net.sourceforge.tess4j.Tesseract;

@Service
public class OcrService {

    public String extractText(MultipartFile file) throws Exception {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("tessdata"); // or your path
        tesseract.setLanguage("eng");

        return tesseract.doOCR(convert(file));
    }

    // convert multipart file → image
    private java.io.File convert(MultipartFile f) throws Exception {
        java.io.File conv = java.io.File.createTempFile("ocr", ".tmp");
        f.transferTo(conv);
        return conv;
    }
}
