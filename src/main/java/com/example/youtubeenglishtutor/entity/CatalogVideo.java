package com.example.youtubeenglishtutor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "catalog_videos",
        indexes = {
                @Index(name = "idx_catalog_videos_category_active", columnList = "category,active"),
                @Index(name = "idx_catalog_videos_video_id", columnList = "video_id")
        }
)
public class CatalogVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private CatalogCategory category;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "video_url", nullable = false)
    private String videoUrl;

    @Column(nullable = false)
    private String title;

    @Column(name = "channel_title")
    private String channelTitle;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "captions_available")
    private Boolean captionsAvailable;

    private String difficulty;

    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "refreshed_at")
    private LocalDateTime refreshedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "source_query", columnDefinition = "TEXT")
    private String sourceQuery;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CatalogCategory getCategory() {
        return category;
    }

    public void setCategory(CatalogCategory category) {
        this.category = category;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Boolean getCaptionsAvailable() {
        return captionsAvailable;
    }

    public void setCaptionsAvailable(Boolean captionsAvailable) {
        this.captionsAvailable = captionsAvailable;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getRefreshedAt() {
        return refreshedAt;
    }

    public void setRefreshedAt(LocalDateTime refreshedAt) {
        this.refreshedAt = refreshedAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getSourceQuery() {
        return sourceQuery;
    }

    public void setSourceQuery(String sourceQuery) {
        this.sourceQuery = sourceQuery;
    }
}
