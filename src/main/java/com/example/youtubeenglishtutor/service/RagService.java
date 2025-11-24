package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.Question;
import com.example.youtubeenglishtutor.entity.Test;
import com.example.youtubeenglishtutor.entity.TranscriptChunk;
import com.example.youtubeenglishtutor.repository.TranscriptChunkRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RagService {

    private final TranscriptChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final int maxSnippetLength;

    public RagService(
            TranscriptChunkRepository chunkRepository,
            EmbeddingService embeddingService,
            @Value("${app.rag.max-snippet-length:400}") int maxSnippetLength) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.maxSnippetLength = maxSnippetLength;
    }

    public String findBestSnippet(Test test, Question question) {
        List<TranscriptChunk> chunks = chunkRepository.findByTestId(test.getId());
        if (chunks.isEmpty()) {
            return "No transcript available";
        }
        String queryText = buildQuery(question);
        List<Double> queryEmbedding = embeddingService.embed(queryText);
        TranscriptChunk best = chunks.stream()
                .max(Comparator.comparingDouble(chunk -> cosine(queryEmbedding, parseEmbedding(chunk.getEmbedding()))))
                .orElse(null);
        if (best == null || !StringUtils.hasText(best.getContent())) {
            return "No transcript available";
        }
        return abbreviate(best.getContent().trim(), maxSnippetLength);
    }

    public void saveChunks(Test test, String transcript, int chunkSize, int overlap) {
        chunkRepository.deleteByTestId(test.getId());
        List<String> parts = chunkTranscript(transcript, chunkSize, overlap);
        List<TranscriptChunk> chunks = parts.stream()
                .map(part -> buildChunk(test, part))
                .collect(Collectors.toList());
        chunkRepository.saveAll(chunks);
    }

    private TranscriptChunk buildChunk(Test test, String content) {
        TranscriptChunk chunk = new TranscriptChunk();
        chunk.setTest(test);
        chunk.setContent(content);
        List<Double> embedding = embeddingService.embed(content);
        chunk.setEmbedding(toString(embedding));
        return chunk;
    }

    private List<String> chunkTranscript(String transcript, int size, int overlap) {
        if (!StringUtils.hasText(transcript)) {
            return List.of();
        }
        String normalized = transcript.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= size) {
            return List.of(normalized);
        }
        int step = Math.max(50, size - overlap);
        List<String> parts = new java.util.ArrayList<>();
        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(normalized.length(), start + size);
            parts.add(normalized.substring(start, end));
            if (end == normalized.length()) {
                break;
            }
        }
        return parts.stream().filter(p -> !p.isBlank()).collect(Collectors.toList());
    }

    private String buildQuery(Question question) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(question.getText())) {
            sb.append(question.getText()).append(" ");
        }
        if (StringUtils.hasText(question.getCorrectAnswer())) {
            sb.append(question.getCorrectAnswer());
        }
        return sb.toString().trim();
    }

    private List<Double> parseEmbedding(String stored) {
        if (!StringUtils.hasText(stored)) {
            return List.of();
        }
        String[] parts = stored.split(",");
        return java.util.Arrays.stream(parts)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }

    private String toString(List<Double> embedding) {
        return embedding.stream()
                .map(d -> String.format(java.util.Locale.US, "%.6f", d))
                .collect(Collectors.joining(","));
    }

    private double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return -1;
        }
        int len = Math.min(a.size(), b.size());
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < len; i++) {
            double x = a.get(i);
            double y = b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB) + 1e-9;
        return dot / denom;
    }

    private String abbreviate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
