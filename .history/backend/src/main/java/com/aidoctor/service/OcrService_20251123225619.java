package com.aidoctor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.IOException;
import java.util.*;

@Service
public class OcrService {

    @Value("${aws.s3.bucket}")
    private String bucket;

    private final TextractClient textract;
    private final S3Client s3;

    public OcrService(TextractClient textract, S3Client s3) {
        this.textract = textract;
        this.s3 = s3;
    }

    public Map<String, Object> extractTextAsJson(MultipartFile file) throws IOException {

        // 1. Upload file to S3
        String key = "ocr/" + System.currentTimeMillis() + "-" + file.getOriginalFilename();

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        // 2. Build Textract S3Object (IMPORTANT: use textract model S3Object)
        software.amazon.awssdk.services.textract.model.S3Object textractS3Object =
                software.amazon.awssdk.services.textract.model.S3Object.builder()
                        .bucket(bucket)
                        .name(key)
                        .build();

        Document document = Document.builder()
                .s3Object(textractS3Object)
                .build();

        // 3. Run Textract: FORMS + TABLES + OCR
        AnalyzeDocumentRequest request = AnalyzeDocumentRequest.builder()
                .featureTypes(FeatureType.TABLES, FeatureType.FORMS)
                .document(document)
                .build();

        AnalyzeDocumentResponse response = textract.analyzeDocument(request);

        // 4. Extract raw text
        StringBuilder fullText = new StringBuilder();
        for (Block block : response.blocks()) {
            if (block.blockType() == BlockType.LINE && block.text() != null) {
                fullText.append(block.text()).append("\n");
            }
        }

        // 5. Extract key-value pairs
        Map<String, String> fields = new HashMap<>();

        for (Block block : response.blocks()) {
            if (block.blockType() == BlockType.KEY_VALUE_SET &&
                    block.entityTypes().contains("KEY")) {

                String keyName = "";
                String valueText = "";

                if (block.relationships() != null) {
                    for (Relationship rel : block.relationships()) {

                        // Extract KEY text
                        if (rel.type() == RelationshipType.CHILD) {
                            for (String id : rel.ids()) {
                                Block child = getBlockById(response, id);
                                if (child != null && child.text() != null) {
                                    keyName += child.text() + " ";
                                }
                            }
                        }

                        // Extract VALUE text
                        if (rel.type() == RelationshipType.VALUE) {
                            for (String id : rel.ids()) {
                                Block valueBlock = getBlockById(response, id);
                                if (valueBlock != null && valueBlock.relationships() != null) {

                                    for (Relationship valRel : valueBlock.relationships()) {
                                        if (valRel.type() == RelationshipType.CHILD) {
                                            for (String cid : valRel.ids()) {
                                                Block child = getBlockById(response, cid);
                                                if (child != null && child.text() != null) {
                                                    valueText += child.text() + " ";
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!keyName.isBlank() && !valueText.isBlank()) {
                    fields.put(keyName.trim(), valueText.trim());
                }
            }
        }

        // 6. Final JSON Response
        return Map.of(
                "status", "success",
                "text", fullText.toString(),
                "fields", fields,
                "pages", response.documentMetadata().pages(),
                "s3Key", key
        );
    }

    // Helper: find block by ID
    private Block getBlockById(AnalyzeDocumentResponse response, String id) {
        return response.blocks().stream()
                .filter(b -> b.id().equals(id))
                .findFirst()
                .orElse(null);
    }
}
