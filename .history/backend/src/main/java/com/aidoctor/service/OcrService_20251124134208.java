package com.aidoctor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.IOException;
import java.util.List;

@Service
public class OcrService {

    private final TextractClient textract;

    public OcrService() {

        // 💡 REPLACE with your actual AWS keys or keep env variables
        AwsBasicCredentials creds = AwsBasicCredentials.create(
                System.getenv("AWS_ACCESS_KEY_ID"),
                System.getenv("AWS_SECRET_ACCESS_KEY")
        );

        this.textract = TextractClient.builder()
                .region(Region.AP_SOUTH_1)        // change region if needed
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
    }

    /** 
     *  Extracts plain text from image/PDF using AWS Textract
     */
    public String extractText(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return "ERROR: No file provided.";
        }

        // Convert file to bytes
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

        List<Block> blocks = response.blocks();
        for (Block b : blocks) {
            if (b.blockType() == BlockType.LINE) {
                sb.append(b.text()).append("\n");
            }
        }

        return sb.toString().trim();
    }
}
