package com.example.youtubeenglishtutor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_preparations")
public class CatalogPreparation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_video_id", nullable = false, unique = true)
    private CatalogVideo catalogVideo;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    private Boolean transcriptReady = false;
    private Boolean embeddingsReady = false;

    private Integer chunkCount;

    private LocalDateTime preparedAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    public Long getId() {
        return id;
    }

    public CatalogVideo getCatalogVideo() {
        return catalogVideo;
    }

    public void setCatalogVideo(CatalogVideo catalogVideo) {
        this.catalogVideo = catalogVideo;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public Boolean getTranscriptReady() {
        return transcriptReady;
    }

    public void setTranscriptReady(Boolean transcriptReady) {
        this.transcriptReady = transcriptReady;
    }

    public Boolean getEmbeddingsReady() {
        return embeddingsReady;
    }

    public void setEmbeddingsReady(Boolean embeddingsReady) {
        this.embeddingsReady = embeddingsReady;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public LocalDateTime getPreparedAt() {
        return preparedAt;
    }

    public void setPreparedAt(LocalDateTime preparedAt) {
        this.preparedAt = preparedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}

