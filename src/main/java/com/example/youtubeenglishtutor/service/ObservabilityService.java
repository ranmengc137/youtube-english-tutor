package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.ObservabilityEvent;
import com.example.youtubeenglishtutor.repository.ObservabilityEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityService.class);

    private final ObservabilityEventRepository repository;
    private final ObjectMapper objectMapper;

    public ObservabilityService(ObservabilityEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public MetricsSnapshot buildMetricsSnapshot(int lookbackDays) {
        LocalDateTime since = LocalDate.now().minusDays(lookbackDays).atStartOfDay();
        List<ObservabilityEvent> recent = repository.findByCreatedAtAfter(since);

        long totalEvents = repository.count();
        long eventsLastPeriod = recent.size();
        long uniqueLearners = repository.countDistinctLearnersSince(since);

        long retrievalEvents = recent.stream()
                .filter(e -> "RETRIEVAL".equalsIgnoreCase(e.getEventType()))
                .count();
        long emptyRetrievalEvents = recent.stream()
                .filter(e -> "RETRIEVAL".equalsIgnoreCase(e.getEventType()))
                .filter(e -> Boolean.TRUE.equals(e.getRetrievalEmpty()))
                .count();
        Double avgRetrievalLatencyMs = repository.averageLatencySince("RETRIEVAL", since);

        long judgeEvents = recent.stream()
                .filter(e -> "JUDGE".equalsIgnoreCase(e.getEventType()))
                .count();
        long judgeCorrect = recent.stream()
                .filter(e -> "JUDGE".equalsIgnoreCase(e.getEventType()))
                .filter(e -> "CORRECT".equalsIgnoreCase(e.getJudgeResult()))
                .count();
        double judgeAccuracy = judgeEvents == 0 ? 0.0 : ((double) judgeCorrect / (double) judgeEvents);

        long feedbackCount = recent.stream()
                .filter(e -> StringUtils.hasText(e.getFeedback()))
                .count();

        List<ObservabilityEvent> latest = repository.findTop20ByOrderByCreatedAtDesc();
        return new MetricsSnapshot(
                LocalDateTime.now(),
                totalEvents,
                eventsLastPeriod,
                uniqueLearners,
                retrievalEvents,
                emptyRetrievalEvents,
                avgRetrievalLatencyMs,
                judgeEvents,
                judgeAccuracy,
                feedbackCount,
                latest
        );
    }

    @Transactional
    public void logRetrievalEvent(
            String learnerId,
            Long testId,
            Long questionId,
            List<Map<String, Object>> topK,
            long latencyMs,
            boolean emptyRetrieval,
            String strategy) {
        ObservabilityEvent event = new ObservabilityEvent();
        event.setLearnerId(learnerId);
        event.setTestId(testId);
        event.setQuestionId(questionId);
        event.setEventType("RETRIEVAL");
        event.setLatencyMs(latencyMs);
        event.setRetrievalEmpty(emptyRetrieval);
        event.setPayload(toJson(Map.of(
                "strategy", strategy,
                "topK", topK
        )));
        persistQuietly(event);
    }

    @Transactional
    public void logJudgeEvent(
            String learnerId,
            Long testId,
            Long questionId,
            String judgeResult,
            String userAnswer) {
        ObservabilityEvent event = new ObservabilityEvent();
        event.setLearnerId(learnerId);
        event.setTestId(testId);
        event.setQuestionId(questionId);
        event.setEventType("JUDGE");
        event.setJudgeResult(judgeResult);
        event.setPayload(toJson(Map.of(
                "userAnswer", userAnswer
        )));
        persistQuietly(event);
    }

    private void persistQuietly(ObservabilityEvent event) {
        try {
            repository.save(event);
        } catch (Exception e) {
            // Keep main user flow resilient even if logging fails.
            log.warn("Failed to persist observability event {}", event.getEventType(), e);
        }
    }

    @Transactional
    public void logFeedback(String learnerId, Long testId, Long questionId, String feedback, String tag) {
        ObservabilityEvent event = new ObservabilityEvent();
        event.setLearnerId(learnerId);
        event.setTestId(testId);
        event.setQuestionId(questionId);
        event.setEventType("FEEDBACK");
        event.setFeedback(feedback);
        event.setPayload(toJson(Map.of("tag", tag)));
        persistQuietly(event);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
