package com.aidoctor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final S3Client s3;

    public FileService(S3Client s3) {
        this.s3 = s3;
    }

    public String saveFile(MultipartFile file) throws IOException {
        String key = "reports/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        return key;
    }

    public ResponseInputStream<GetObjectResponse> getFile(String key) {
        return s3.getObject(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()
        );
    }
}
