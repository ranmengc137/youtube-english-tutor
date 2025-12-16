package com.example.youtubeenglishtutor.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class YtDlpVideoMetadataService implements VideoMetadataService {

    private static final Logger log = LoggerFactory.getLogger(YtDlpVideoMetadataService.class);

    @Value("${app.ytdlp.binary:yt-dlp}")
    private String ytdlpBinary;

    @Override
    public long getDurationSeconds(String videoUrl) {
        try {
            List<String> command = List.of(
                    ytdlpBinary,
                    "--get-duration",
                    "--no-playlist",
                    videoUrl
            );
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true) // merge stderr to avoid losing duration line amid warnings
                    .start();
            int exit = process.waitFor();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String durationLine = extractDurationLine(output);
            if (exit != 0 || !StringUtils.hasText(durationLine)) {
                log.warn("yt-dlp duration lookup failed exit={} rawOutput={}", exit, output);
                return -1;
            }
            return parseDuration(durationLine.trim());
        } catch (Exception e) {
            log.warn("Failed to query duration via yt-dlp for {}", videoUrl, e);
            return -1;
        }
    }

    @Override
    public String getTitle(String videoUrl) {
        try {
            List<String> command = List.of(
                    ytdlpBinary,
                    "--get-title",
                    "--no-playlist",
                    videoUrl
            );
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            int exit = process.waitFor();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String title = extractFirstNonBlankLine(output);
            if (exit != 0 || !StringUtils.hasText(title)) {
                log.warn("yt-dlp title lookup failed exit={} rawOutput={}", exit, output);
                return null;
            }
            return title.trim();
        } catch (Exception e) {
            log.warn("Failed to query title via yt-dlp for {}", videoUrl, e);
            return null;
        }
    }

    private long parseDuration(String duration) {
        String[] parts = duration.split(":");
        try {
            if (parts.length == 3) {
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int s = Integer.parseInt(parts[2]);
                return h * 3600L + m * 60L + s;
            } else if (parts.length == 2) {
                int m = Integer.parseInt(parts[0]);
                int s = Integer.parseInt(parts[1]);
                return m * 60L + s;
            } else {
                return Long.parseLong(duration);
            }
        } catch (NumberFormatException e) {
            log.warn("Unable to parse duration string: {}", duration);
            return -1;
        }
    }

    private String extractDurationLine(String rawOutput) {
        if (!StringUtils.hasText(rawOutput)) {
            return null;
        }
        String durationLine = null;
        for (String line : rawOutput.split("\\R")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            // pick the last line that looks like a duration; yt-dlp may emit warnings first.
            if (trimmed.matches("^\\d+$") || trimmed.matches("^\\d{1,2}:\\d{2}(:\\d{2})?$")) {
                durationLine = trimmed;
            }
        }
        return durationLine;
    }

    private String extractFirstNonBlankLine(String rawOutput) {
        if (!StringUtils.hasText(rawOutput)) {
            return null;
        }
        for (String line : rawOutput.split("\\R")) {
            String trimmed = line.trim();
            if (StringUtils.hasText(trimmed)) {
                return trimmed;
            }
        }
        return null;
    }
}
