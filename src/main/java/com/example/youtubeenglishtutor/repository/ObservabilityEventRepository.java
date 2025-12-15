package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.ObservabilityEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObservabilityEventRepository extends JpaRepository<ObservabilityEvent, Long> {

    long countByEventType(String eventType);

    long countByEventTypeAndRetrievalEmptyTrue(String eventType);

    long countByEventTypeAndJudgeResult(String eventType, String judgeResult);

    List<ObservabilityEvent> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<ObservabilityEvent> findByCreatedAtAfter(LocalDateTime since);

    List<ObservabilityEvent> findTop20ByOrderByCreatedAtDesc();

    @Query("select avg(e.latencyMs) from ObservabilityEvent e where e.eventType = :eventType and e.latencyMs is not null and e.createdAt >= :since")
    Double averageLatencySince(@Param("eventType") String eventType, @Param("since") LocalDateTime since);

    @Query("select count(distinct e.learnerId) from ObservabilityEvent e where e.createdAt >= :since")
    long countDistinctLearnersSince(@Param("since") LocalDateTime since);
}
