package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.ObservabilityEvent;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Simple DTO used by the admin metrics page.
 */
public record MetricsSnapshot(
        LocalDateTime generatedAt,
        long totalEvents,
        long eventsLastPeriod,
        long uniqueLearnersLastPeriod,
        long retrievalEvents,
        long emptyRetrievalEvents,
        Double avgRetrievalLatencyMs,
        long judgeEvents,
        double judgeAccuracy,
        long feedbackCount,
        List<ObservabilityEvent> recentEvents) {
}
