package com.example.youtubeenglishtutor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "observability_events")
public class ObservabilityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;

    private String learnerId;

    private Long testId;

    private Long questionId;

    private String eventType;

    private Long latencyMs;

    private Integer tokenUsage;

    private Boolean retrievalEmpty;

    private String judgeResult;

    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Integer getTokenUsage() {
        return tokenUsage;
    }

    public void setTokenUsage(Integer tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    public Boolean getRetrievalEmpty() {
        return retrievalEmpty;
    }

    public void setRetrievalEmpty(Boolean retrievalEmpty) {
        this.retrievalEmpty = retrievalEmpty;
    }

    public String getJudgeResult() {
        return judgeResult;
    }

    public void setJudgeResult(String judgeResult) {
        this.judgeResult = judgeResult;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
