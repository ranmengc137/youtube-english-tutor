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
            Process process = new ProcessBuilder(command).start();
            int exit = process.waitFor();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (exit != 0 || !StringUtils.hasText(output)) {
                log.warn("yt-dlp duration lookup failed exit={} output={}", exit, output);
                return -1;
            }
            return parseDuration(output);
        } catch (Exception e) {
            log.warn("Failed to query duration via yt-dlp for {}", videoUrl, e);
            return -1;
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
}
