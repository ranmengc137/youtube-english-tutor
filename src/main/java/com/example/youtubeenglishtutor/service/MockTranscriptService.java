package com.example.youtubeenglishtutor.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "app.transcript.provider", havingValue = "mock", matchIfMissing = true)
public class MockTranscriptService implements TranscriptService {

    @Override
    public String fetchTranscript(String videoUrl) {
        return "Sample transcript for video: " + videoUrl
                + ". The speaker shares tips on learning English effectively with practice and repetition.";
    }
}
