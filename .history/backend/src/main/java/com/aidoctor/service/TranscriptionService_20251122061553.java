package com.aidoctor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class TranscriptionService {

    public String transcribeAudio(MultipartFile audio) {
        try {
            File tmp = File.createTempFile("audio-", ".tmp");
            audio.transferTo(tmp);
            // TODO: call real transcription (AWS Transcribe / Whisper) here
            return "MOCK TRANSCRIPTION: audio saved at " + tmp.getAbsolutePath();
        } catch (IOException e) {
            return "Error saving audio: " + e.getMessage();
        }
    }
}
