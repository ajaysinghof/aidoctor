package com.aidoctor.controller;

import com.aidoctor.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String fileKey = fileService.saveFile(file);
            return ResponseEntity.ok(fileKey);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> downloadFile(@PathVariable String key) {
        try {
            ResponseInputStream<GetObjectResponse> s3Object = fileService.getFile(key);

            byte[] bytes = s3Object.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Download failed: " + e.getMessage());
        }
    }
}
