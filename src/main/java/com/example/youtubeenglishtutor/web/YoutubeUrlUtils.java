package com.example.youtubeenglishtutor.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class YoutubeUrlUtils {

    private static final Pattern WATCH_PATTERN = Pattern.compile("[?&]v=([^&]+)");
    private static final Pattern SHORT_PATTERN = Pattern.compile("youtu\\.be/([^?&/]+)");
    private static final Pattern EMBED_PATTERN = Pattern.compile("/embed/([^?&/]+)");

    private YoutubeUrlUtils() {
    }

    public static String extractVideoId(String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return null;
        }
        Matcher watchMatcher = WATCH_PATTERN.matcher(videoUrl);
        if (watchMatcher.find()) {
            return watchMatcher.group(1);
        }
        Matcher shortMatcher = SHORT_PATTERN.matcher(videoUrl);
        if (shortMatcher.find()) {
            return shortMatcher.group(1);
        }
        Matcher embedMatcher = EMBED_PATTERN.matcher(videoUrl);
        if (embedMatcher.find()) {
            return embedMatcher.group(1);
        }
        return null;
    }
}

