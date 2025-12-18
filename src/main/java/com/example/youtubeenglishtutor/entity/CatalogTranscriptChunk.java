package com.example.youtubeenglishtutor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_transcript_chunks")
public class CatalogTranscriptChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_video_id", nullable = false)
    private CatalogVideo catalogVideo;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    public Long getId() {
        return id;
    }

    public CatalogVideo getCatalogVideo() {
        return catalogVideo;
    }

    public void setCatalogVideo(CatalogVideo catalogVideo) {
        this.catalogVideo = catalogVideo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }
}

