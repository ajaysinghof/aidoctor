package com.aidoctor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Minimal FileService stub that does NOT actually upload to S3 — just returns a simple key and provides a fake download stream.
 * Replace with your S3 logic later.
 */
@Service
public class FileService {

    public String saveFile(MultipartFile file) throws Exception {
        // quick local key
        return file == null ? "no-file" : file.getOriginalFilename();
    }

    public ResponseInputStream<GetObjectResponse> getFile(String key) throws Exception {
        // Not implemented: throw for now so callers know. Or you can return a fake stream if needed.
        throw new UnsupportedOperationException("S3 download not configured in minimal mode.");
    }
}
