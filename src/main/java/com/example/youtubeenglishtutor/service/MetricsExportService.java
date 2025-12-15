package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.ObservabilityEvent;
import com.example.youtubeenglishtutor.repository.ObservabilityEventRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes a daily CSV of observability events so we can analyze sessions offline.
 */
@Service
public class MetricsExportService {

    private static final Logger log = LoggerFactory.getLogger(MetricsExportService.class);

    private final ObservabilityEventRepository repository;

    @Value("${app.metrics.export-dir:logs}")
    private String exportDir;

    public MetricsExportService(ObservabilityEventRepository repository) {
        this.repository = repository;
    }

    // Run shortly after midnight local time.
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 5 0 * * *")
    public void exportPreviousDay() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try {
            Path exported = exportForDate(yesterday);
            log.info("Metrics export written for {} -> {}", yesterday, exported.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to write metrics export for {}", yesterday, e);
        }
    }

    /**
     * Export all observability events for a given day to CSV. Exposed for manual triggers/tests.
     */
    @Transactional(readOnly = true)
    public Path exportForDate(LocalDate day) throws IOException {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        List<ObservabilityEvent> events = repository.findByCreatedAtBetween(start, end);

        Files.createDirectories(Paths.get(exportDir));
        Path file = Paths.get(exportDir, "observability-" + day + ".csv");

        String header = String.join(",",
                "created_at",
                "learner_id",
                "test_id",
                "question_id",
                "event_type",
                "latency_ms",
                "token_usage",
                "retrieval_empty",
                "judge_result",
                "feedback",
                "payload"
        );

        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(header);
            writer.newLine();
            for (ObservabilityEvent e : events) {
                writer.write(String.join(",",
                        csv(e.getCreatedAt() != null ? e.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : ""),
                        csv(e.getLearnerId()),
                        csv(e.getTestId()),
                        csv(e.getQuestionId()),
                        csv(e.getEventType()),
                        csv(e.getLatencyMs()),
                        csv(e.getTokenUsage()),
                        csv(e.getRetrievalEmpty()),
                        csv(e.getJudgeResult()),
                        csv(e.getFeedback()),
                        csv(e.getPayload())
                ));
                writer.newLine();
            }
        }
        return file;
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString();
        // basic CSV escaping for commas/quotes/newlines.
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
