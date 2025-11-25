package com.aidoctor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OcrService {

    private final TextractClient textract;

    /**
     * SAFE constructor → reads keys from application.properties
     */
    public OcrService(
            @Value("${aws.accessKeyId}") String accessKey,
            @Value("${aws.secretAccessKey}") String secretKey,
            @Value("${aws.region}") String region
    ) {

        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);

        this.textract = TextractClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
    }

    /**
     * Core Textract OCR method → returns plain extracted text
     */
    public String extractText(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return "ERROR: No file provided.";
        }

        byte[] bytes = file.getBytes();
        SdkBytes sdkBytes = SdkBytes.fromByteArray(bytes);

        Document document = Document.builder()
                .bytes(sdkBytes)
                .build();

        DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                .document(document)
                .build();

        DetectDocumentTextResponse response = textract.detectDocumentText(request);

        StringBuilder sb = new StringBuilder();

        for (Block block : response.blocks()) {
            if (block.blockType() == BlockType.LINE) {
                sb.append(block.text()).append("\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * REQUIRED BY UploadController + ReportService
     * Returns: { fileName, text }
     */
    public Map<String, Object> extractTextAsJson(MultipartFile file) throws IOException {
        String text = extractText(file);

        Map<String, Object> map = new HashMap<>();
        map.put("fileName", file.getOriginalFilename());
        map.put("text", text);

        return map;
    }

    /**
     * REQUIRED BY OcrController
     * Simple wrapper
     */
    public Map<String, String> process(MultipartFile file) throws IOException {

        String text = extractText(file);

        return Map.of(
                "fileName", file.getOriginalFilename(),
                "text", text,
                "summary", "OCR extraction completed"
        );
    }
}
