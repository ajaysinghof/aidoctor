package com.aidoctor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * Uploads file to S3 and runs AnalyzeDocument, returning a map:
     * { "text": "...", "fields": Map<String,String>, "s3Key": "...", "documentMetadata": ... }
     */
    public Map<String,Object> extractTextAsJson(MultipartFile file) throws IOException {
        String key = "ocr/" + System.currentTimeMillis() + "-" + Objects.requireNonNull(file.getOriginalFilename());

        // upload to S3
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/pdf")
                .build();

        s3.putObject(putReq, RequestBody.fromBytes(file.getBytes()));

        // build Textract Document pointing to S3 (use Textract's S3Object builder explicitly)
        software.amazon.awssdk.services.textract.model.S3Object texS3 = software.amazon.awssdk.services.textract.model.S3Object.builder()
                .bucket(bucket)
                .name(key)
                .build();

        Document doc = Document.builder()
                .s3Object(texS3)
                .build();

        AnalyzeDocumentRequest req = AnalyzeDocumentRequest.builder()
                .featureTypes(FeatureType.TABLES, FeatureType.FORMS)
                .document(doc)
                .build();

        AnalyzeDocumentResponse resp = textract.analyzeDocument(req);

        // Extract lines
        StringBuilder fullText = new StringBuilder();
        for (Block b : resp.blocks()) {
            if (b.blockType() == BlockType.LINE && b.text() != null) {
                fullText.append(b.text()).append("\n");
            }
        }

        // Extract key-value pairs (Textract KEY/VALUE blocks)
        Map<String,String> fields = new LinkedHashMap<>();
        Map<String, Block> byId = resp.blocks().stream().collect(Collectors.toMap(Block::id, b -> b));

        for (Block block : resp.blocks()) {
            if (block.blockType() == BlockType.KEY_VALUE_SET && block.entityTypes() != null && block.entityTypes().contains("KEY")) {
                String keyName = "";
                String valueText = "";

                if (block.relationships() != null) {
                    for (Relationship rel : block.relationships()) {
                        if (rel.type() == RelationshipType.CHILD) {
                            for (String id : rel.ids()) {
                                Block child = byId.get(id);
                                if (child != null && child.text() != null) {
                                    keyName += child.text() + " ";
                                }
                            }
                        } else if (rel.type() == RelationshipType.VALUE) {
                            for (String id : rel.ids()) {
                                Block vblock = byId.get(id);
                                if (vblock != null && vblock.relationships() != null) {
                                    // find child text blocks under value block
                                    for (Relationship vrel : vblock.relationships()) {
                                        if (vrel.type() == RelationshipType.CHILD) {
                                            for (String cid : vrel.ids()) {
                                                Block c = byId.get(cid);
                                                if (c != null && c.text() != null) valueText += c.text() + " ";
                                            }
                                        }
                                    }
                                    if (vblock.text() != null && !vblock.text().isBlank()) valueText += vblock.text();
                                }
                                if (vblock != null && vblock.text() != null && valueText.isBlank()) {
                                    valueText += vblock.text();
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

        Map<String,Object> out = new HashMap<>();
        out.put("text", fullText.toString());
        out.put("fields", fields);
        out.put("s3Key", key);
        out.put("documentMetadata", resp.documentMetadata());

        return out;
    }
}
