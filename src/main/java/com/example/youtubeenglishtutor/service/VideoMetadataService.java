package com.example.youtubeenglishtutor.service;

public interface VideoMetadataService {
    /**
     * Returns the duration in seconds, or -1 if unknown.
     */
    long getDurationSeconds(String videoUrl);
}
