package com.aidoctor.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;


/**
 * OcrService - uses AWS Textract to extract text lines.
 * Reads AWS config from application.properties or environment variables.
 */
@Service
public class OcrService {

    @Value("${aws.accessKeyId:}")
    private String awsAccessKeyId;

    @Value("${aws.secretAccessKey:}")
    private String awsSecretAccessKey;

    @Value("${aws.region:ap-south-1}")
    private String awsRegion;

    private TextractClient textract;

    @PostConstruct
    public void init() {
        // prefer properties; fall back to env vars
        if ((awsAccessKeyId == null || awsAccessKeyId.isBlank()) ||
                (awsSecretAccessKey == null || awsSecretAccessKey.isBlank())) {

            String envKey = System.getenv("AWS_ACCESS_KEY_ID");
            String envSecret = System.getenv("AWS_SECRET_ACCESS_KEY");
            String envRegion = System.getenv("AWS_REGION");

            if ((envKey != null && !envKey.isBlank()) && (envSecret != null && !envSecret.isBlank())) {
                awsAccessKeyId = envKey;
                awsSecretAccessKey = envSecret;
            } else {
                // Textract won't be available — leave textract null and report helpful message when used
                this.textract = null;
                return;
            }

            if (envRegion != null && !envRegion.isBlank()) awsRegion = envRegion;
        }

        AwsBasicCredentials creds = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
        this.textract = TextractClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
    }

    /**
     * Extracts plain text from image/PDF using Textract.
     * Throws IllegalStateException if Textract not configured.
     */
    public String extractText(MultipartFile file) throws IOException {
        if (this.textract == null) {
            throw new IllegalStateException(
                    "Textract client not configured. Please set aws.accessKeyId and aws.secretAccessKey (or environment variables).");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        byte[] bytes = file.getBytes();
        SdkBytes sdkBytes = SdkBytes.fromByteArray(bytes);

        Document document = Document.builder().bytes(sdkBytes).build();

        // Try DetectDocumentText (works for images). For PDFs you may need StartDocumentTextDetection (async).
        DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                .document(document)
                .build();

        DetectDocumentTextResponse response;
        try {
            response = textract.detectDocumentText(request);
        } catch (TextractException te) {
            // wrap for controller to report
            throw new IOException("Textract error: " + te.awsErrorDetails().errorMessage(), te);
        }

        StringBuilder sb = new StringBuilder();
        List<Block> blocks = response.blocks();
        if (blocks != null) {
            for (Block b : blocks) {
                if (b.blockType() == BlockType.LINE && b.text() != null) {
                    sb.append(b.text()).append("\n");
                }
            }
        }

        String extracted = sb.toString().trim();
        if (extracted.isEmpty()) extracted = "(no text extracted)";

        return extracted;
    }
}
