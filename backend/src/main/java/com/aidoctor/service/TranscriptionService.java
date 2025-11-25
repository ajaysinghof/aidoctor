package com.aidoctor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TranscriptionService {
    public String transcribeAudio(MultipartFile audio) {
        if (audio == null) return "";
        return "Transcription stub for " + audio.getOriginalFilename();
    }
}
