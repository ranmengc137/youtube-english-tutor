package com.example.youtubeenglishtutor.service;

import java.time.Duration;

public final class YoutubeDurationParser {

    private YoutubeDurationParser() {
    }

    public static long parseSeconds(String iso8601Duration) {
        if (iso8601Duration == null || iso8601Duration.isBlank()) {
            return -1;
        }
        try {
            Duration duration = Duration.parse(iso8601Duration.trim());
            return duration.getSeconds();
        } catch (Exception ignored) {
            return -1;
        }
    }
}

