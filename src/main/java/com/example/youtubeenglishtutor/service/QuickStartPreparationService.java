package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.Test;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QuickStartPreparationService {

    private static final Logger log = LoggerFactory.getLogger(QuickStartPreparationService.class);

    public record Status(String state, Long testId, String error) {
        public static Status pending() {
            return new Status("PENDING", null, null);
        }

        public static Status ready(long testId) {
            return new Status("READY", testId, null);
        }

        public static Status error(String message) {
            return new Status("ERROR", null, message);
        }
    }

    private final TestService testService;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, CompletableFuture<Status>> futuresByPrepId = new ConcurrentHashMap<>();
    private final Map<String, String> prepIdByKey = new ConcurrentHashMap<>();

    public QuickStartPreparationService(TestService testService) {
        this.testService = testService;
    }

    public String start(String learnerId, String videoUrl, Integer desiredSize) {
        String key = (learnerId != null ? learnerId : "anon") + "|" + videoUrl + "|" + (desiredSize != null ? desiredSize : "default");
        String existingPrepId = prepIdByKey.get(key);
        if (existingPrepId != null) {
            log.info("Quick-start prep: reuse existing prepId={} learnerId={} videoUrl={}", existingPrepId, learnerId, videoUrl);
            return existingPrepId;
        }
        String prepId = UUID.randomUUID().toString();
        prepIdByKey.put(key, prepId);
        log.info("Quick-start prep: start prepId={} learnerId={} videoUrl={} desiredSize={}", prepId, learnerId, videoUrl, desiredSize);

        CompletableFuture<Status> future = CompletableFuture.supplyAsync(() -> {
            try {
                Test test = testService.createTest(videoUrl, null, true, desiredSize);
                log.info("Quick-start prep: ready prepId={} testId={}", prepId, test.getId());
                return Status.ready(test.getId());
            } catch (Exception e) {
                log.warn("Quick-start prep: error prepId={} msg={}", prepId, e.getMessage());
                return Status.error(e.getMessage() != null ? e.getMessage() : "Failed to prepare quiz.");
            }
        }, executor);

        futuresByPrepId.put(prepId, future);
        // Best-effort cleanup after a while.
        future.orTimeout(Duration.ofMinutes(30).toSeconds(), java.util.concurrent.TimeUnit.SECONDS)
                .exceptionally(ignored -> Status.error("Timed out preparing quiz."))
                .whenComplete((status, ignored) -> {
                    // Keep a short-lived record for polling; removing the key would break refreshes.
                });

        return prepId;
    }

    public Status status(String prepId) {
        CompletableFuture<Status> future = futuresByPrepId.get(prepId);
        if (future == null) {
            log.info("Quick-start prep: status unknown prepId={}", prepId);
            return Status.error("Unknown preparation id.");
        }
        if (!future.isDone()) {
            return Status.pending();
        }
        try {
            return future.getNow(Status.pending());
        } catch (Exception e) {
            return Status.error("Failed to prepare quiz.");
        }
    }
}
