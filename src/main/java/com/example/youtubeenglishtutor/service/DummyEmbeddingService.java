package com.example.youtubeenglishtutor.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "app.embedding.provider", havingValue = "dummy", matchIfMissing = true)
public class DummyEmbeddingService implements EmbeddingService {

    @Override
    public List<Double> embed(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            // Create a small deterministic vector from hash bytes
            double a = (hash[0] & 0xFF) / 255.0;
            double b = (hash[1] & 0xFF) / 255.0;
            double c = (hash[2] & 0xFF) / 255.0;
            double norm = Math.sqrt(a * a + b * b + c * c) + 1e-9;
            return Arrays.asList(a / norm, b / norm, c / norm);
        } catch (Exception e) {
            return Arrays.asList(0.0, 0.0, 0.0);
        }
    }
}
