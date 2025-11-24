package com.example.youtubeenglishtutor.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tests")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;

    private String videoUrl;

    private String videoTitle;

    private Integer score;

    private Integer totalQuestions;

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String transcript;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<WrongQuestion> wrongQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TranscriptChunk> transcriptChunks = new ArrayList<>();

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

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public List<WrongQuestion> getWrongQuestions() {
        return wrongQuestions;
    }

    public void addQuestion(Question question) {
        this.questions.add(question);
        question.setTest(this);
    }

    public void addWrongQuestion(WrongQuestion wrongQuestion) {
        this.wrongQuestions.add(wrongQuestion);
        wrongQuestion.setTest(this);
    }

    public List<TranscriptChunk> getTranscriptChunks() {
        return transcriptChunks;
    }

    public void addTranscriptChunk(TranscriptChunk chunk) {
        this.transcriptChunks.add(chunk);
        chunk.setTest(this);
    }
}
