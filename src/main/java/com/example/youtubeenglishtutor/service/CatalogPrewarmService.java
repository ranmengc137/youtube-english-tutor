package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.CatalogPreparation;
import com.example.youtubeenglishtutor.entity.CatalogTranscriptChunk;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import com.example.youtubeenglishtutor.repository.CatalogPreparationRepository;
import com.example.youtubeenglishtutor.repository.CatalogTranscriptChunkRepository;
import com.example.youtubeenglishtutor.repository.CatalogVideoRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CatalogPrewarmService {

    private static final Logger log = LoggerFactory.getLogger(CatalogPrewarmService.class);

    private final CatalogVideoRepository catalogVideoRepository;
    private final CatalogPreparationRepository catalogPreparationRepository;
    private final CatalogTranscriptChunkRepository chunkRepository;
    private final TranscriptService transcriptService;
    private final EmbeddingService embeddingService;

    @Value("${app.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:100}")
    private int chunkOverlap;

    public CatalogPrewarmService(
            CatalogVideoRepository catalogVideoRepository,
            CatalogPreparationRepository catalogPreparationRepository,
            CatalogTranscriptChunkRepository chunkRepository,
            TranscriptService transcriptService,
            EmbeddingService embeddingService) {
        this.catalogVideoRepository = catalogVideoRepository;
        this.catalogPreparationRepository = catalogPreparationRepository;
        this.chunkRepository = chunkRepository;
        this.transcriptService = transcriptService;
        this.embeddingService = embeddingService;
    }

    public List<CatalogVideo> findNeedingPrewarm(int limit) {
        return catalogVideoRepository.findNeedingPrewarm(PageRequest.of(0, Math.max(1, limit))).getContent();
    }

    @Transactional
    public void prewarm(CatalogVideo video) {
        CatalogPreparation prep = catalogPreparationRepository.findByCatalogVideo(video)
                .orElseGet(() -> {
                    CatalogPreparation p = new CatalogPreparation();
                    p.setCatalogVideo(video);
                    return p;
                });
        try {
            String transcript = transcriptService.fetchTranscript(video.getVideoUrl());
            prep.setTranscript(transcript);
            prep.setTranscriptReady(true);

            chunkRepository.deleteByCatalogVideo(video);
            List<String> parts = chunkTranscript(transcript, chunkSize, chunkOverlap);
            List<CatalogTranscriptChunk> chunks = new ArrayList<>();
            for (String part : parts) {
                CatalogTranscriptChunk chunk = new CatalogTranscriptChunk();
                chunk.setCatalogVideo(video);
                chunk.setContent(part);
                chunk.setEmbedding(toString(embeddingService.embed(part)));
                chunks.add(chunk);
            }
            if (!chunks.isEmpty()) {
                chunkRepository.saveAll(chunks);
            }
            prep.setEmbeddingsReady(true);
            prep.setChunkCount(chunks.size());
            prep.setPreparedAt(LocalDateTime.now());
            prep.setLastError(null);
            catalogPreparationRepository.save(prep);
            log.info("Prewarm OK videoId={} chunks={} duration={}s", video.getVideoId(), chunks.size(), video.getDurationSeconds());
        } catch (Exception e) {
            prep.setEmbeddingsReady(false);
            prep.setPreparedAt(LocalDateTime.now());
            prep.setLastError(e.getMessage());
            catalogPreparationRepository.save(prep);
            log.warn("Prewarm FAILED videoId={} msg={}", video.getVideoId(), e.getMessage());
        }
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
        return parts.stream().filter(p -> !p.isBlank()).toList();
    }

    private String toString(List<Double> embedding) {
        return embedding.stream()
                .map(d -> String.format(java.util.Locale.US, "%.6f", d))
                .collect(java.util.stream.Collectors.joining(","));
    }
}

