package com.example.youtubeenglishtutor.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class YtDlpTranscriptService implements TranscriptService {

    private static final Logger log = LoggerFactory.getLogger(YtDlpTranscriptService.class);

    @Value("${app.ytdlp.binary:yt-dlp}")
    private String ytdlpBinary;

    @Value("${app.ytdlp.extractor-args:youtube:player_client=android}")
    private String extractorArgs;

    @Value("${app.ytdlp.sub-lang:en}")
    private String subLang;

    @Value("${app.ytdlp.sub-format:srt}")
    private String subFormat;

    @Override
    public String fetchTranscript(String videoUrl) {
        try {
            Path tempDir = Files.createTempDirectory("yt-transcripts");
            log.info("Invoking yt-dlp for videoUrl={} tempDir={}", videoUrl, tempDir);
            List<String> command = List.of(
                    ytdlpBinary,
                    "--skip-download",
                    "--write-auto-sub",
                    "--sub-lang", subLang,
                    "--sub-format", subFormat,
                    "--convert-subs", subFormat,
                    "--no-playlist",
                    "--extractor-args", extractorArgs,
                    "-o", tempDir.resolve("%(id)s.%(ext)s").toString(),
                    videoUrl
            );
            log.debug("yt-dlp command: {}", String.join(" ", command));

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

            boolean completed = process.waitFor(Duration.ofMinutes(2).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!completed || process.exitValue() != 0) {
                log.error("yt-dlp failed. completed={} exitCode={} output={}", completed, completed ? process.exitValue() : null, output);
                throw new IllegalStateException("yt-dlp failed. Exit=" + (completed ? process.exitValue() : "timeout")
                        + " Output=" + output);
            }

            Optional<Path> transcriptFile = Files.list(tempDir)
                    .filter(path -> path.toString().endsWith(".srt"))
                    .findFirst();

            if (transcriptFile.isEmpty()) {
                log.error("yt-dlp did not produce an SRT file in {}", tempDir);
                throw new IllegalStateException("No SRT transcript produced by yt-dlp.");
            }

            String srt = Files.readString(transcriptFile.get());
            cleanupTempDir(tempDir);
            log.debug("Transcript file read from {} ({} chars)", transcriptFile.get(), srt.length());
            return stripSrtToPlainText(srt);
        } catch (Exception e) {
            log.error("Failed to fetch transcript via yt-dlp for {}", videoUrl, e);
            throw new IllegalStateException("Failed to fetch transcript via yt-dlp", e);
        }
    }

    private String stripSrtToPlainText(String srtContent) {
        // Remove sequence numbers and timestamps, keep text lines.
        return srtContent.lines()
                .filter(line -> !line.matches("^\\d+$"))
                .filter(line -> !line.matches("^\\d{2}:\\d{2}:\\d{2},\\d{3} --> .*"))
                .filter(line -> !line.isBlank())
                .collect(Collectors.joining(" "));
    }

    private void cleanupTempDir(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
