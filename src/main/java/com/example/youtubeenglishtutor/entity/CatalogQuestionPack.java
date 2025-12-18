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
import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_question_packs")
public class CatalogQuestionPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_video_id", nullable = false)
    private CatalogVideo catalogVideo;

    private Integer size;

    private String difficulty;

    private Boolean includesWriting;

    @Column(columnDefinition = "TEXT")
    private String questionsJson;

    private LocalDateTime createdAt;

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

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getIncludesWriting() {
        return includesWriting;
    }

    public void setIncludesWriting(Boolean includesWriting) {
        this.includesWriting = includesWriting;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public void setQuestionsJson(String questionsJson) {
        this.questionsJson = questionsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}

