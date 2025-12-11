package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.Question;
import com.example.youtubeenglishtutor.entity.QuestionType;
import com.example.youtubeenglishtutor.entity.Test;
import com.example.youtubeenglishtutor.entity.WrongQuestion;
import com.example.youtubeenglishtutor.repository.TestRepository;
import com.example.youtubeenglishtutor.repository.TranscriptChunkRepository;
import com.example.youtubeenglishtutor.repository.WrongQuestionRepository;
import com.example.youtubeenglishtutor.service.VideoMetadataService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RefreshScope
public class TestService {

    private static final Logger log = LoggerFactory.getLogger(TestService.class);

    private final TestRepository testRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final AiQuestionService aiQuestionService;
    private final TranscriptService transcriptService;
    private final RagService ragService;
    private final TranscriptChunkRepository chunkRepository;
    private final VideoMetadataService videoMetadataService;

    @Value("${app.download.default-path:downloads}")
    private String defaultDownloadPath;

    @Value("${app.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:100}")
    private int chunkOverlap;

    @Value("${app.video.max-seconds:1800}")
    private long maxVideoSeconds;

    @Value("${app.video.enforce-limit}")
    private boolean enforceVideoLimit;

    public TestService(
            TestRepository testRepository,
            WrongQuestionRepository wrongQuestionRepository,
            AiQuestionService aiQuestionService,
            TranscriptService transcriptService,
            RagService ragService,
            TranscriptChunkRepository chunkRepository,
            VideoMetadataService videoMetadataService) {
        this.testRepository = testRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.aiQuestionService = aiQuestionService;
        this.transcriptService = transcriptService;
        this.ragService = ragService;
        this.chunkRepository = chunkRepository;
        this.videoMetadataService = videoMetadataService;
    }

    @Transactional
    public Test createTest(String videoUrl, String downloadPath, boolean useDefaultPath) {
        String resolvedPath = resolveDownloadPath(downloadPath, useDefaultPath);
        log.info("Creating test for videoUrl={} using downloadPath={}", videoUrl, resolvedPath);
        Optional<Test> existing = testRepository.findFirstByVideoUrlOrderByCreatedAtDesc(videoUrl);
        if (existing.isPresent()) {
            log.info("Reusing existing test {} for videoUrl={}", existing.get().getId(), videoUrl);
            return existing.get();
        }
        enforceDurationLimit(videoUrl);
        String transcript = fetchOrReuseTranscript(videoUrl, resolvedPath);
        log.debug("Transcript ready ({} chars)", transcript != null ? transcript.length() : 0);

        Test test = new Test();
        test.setVideoUrl(videoUrl);
        test.setVideoTitle("YouTube Video");
        test.setTranscript(transcript);

        List<Question> generatedQuestions = aiQuestionService.generateQuestionsFromTranscript(transcript);
        generatedQuestions.forEach(test::addQuestion);
        test.setTotalQuestions(generatedQuestions.size());
        test = testRepository.save(test);
        ragService.saveChunks(test, transcript, chunkSize, chunkOverlap);

        return testRepository.save(test);
    }

    @Transactional(readOnly = true)
    public List<Test> listTests() {
        return testRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Test getTest(Long id) {
        return testRepository.findById(id).orElse(null);
    }

    @Transactional
    public Test submitAnswers(Long testId, Map<Long, List<String>> answersByQuestionId) {
        Optional<Test> testOpt = testRepository.findById(testId);
        if (testOpt.isEmpty()) {
            log.warn("submitAnswers: test not found id={}", testId);
            return null;
        }
        Test test = testOpt.get();

        wrongQuestionRepository.deleteByTestId(testId);
        test.getWrongQuestions().clear();

        int score = 0;
        List<Question> questions = test.getQuestions();
        for (Question question : questions) {
            List<String> submittedAnswers = answersByQuestionId.getOrDefault(question.getId(), Collections.emptyList());
            boolean correct = evaluateAnswer(question, submittedAnswers);
            if (correct) {
                score++;
            } else {
                log.debug("Question {} answered incorrectly. User answers={}", question.getId(), submittedAnswers);
                WrongQuestion wrongQuestion = new WrongQuestion();
                wrongQuestion.setQuestion(question);
                wrongQuestion.setUserAnswer(String.join(";", submittedAnswers));
                test.addWrongQuestion(wrongQuestion);
            }
        }

        test.setScore(score);
        test.setTotalQuestions(questions.size());
        return testRepository.save(test);
    }

    private boolean evaluateAnswer(Question question, List<String> submittedAnswers) {
        List<String> correctAnswers = question.getCorrectAnswerList().stream()
                .map(answer -> answer.toLowerCase().trim())
                .toList();
        List<String> sanitizedUserAnswers = submittedAnswers.stream()
                .map(ans -> ans.toLowerCase().trim())
                .filter(StringUtils::hasText)
                .toList();

        QuestionType type = question.getType();
        switch (type) {
            case SINGLE_CHOICE:
            case TRUE_FALSE:
                return sanitizedUserAnswers.size() == 1 && correctAnswers.containsAll(sanitizedUserAnswers);
            case MULTIPLE_CHOICE:
                Set<String> correctSet = Set.copyOf(correctAnswers);
                Set<String> submittedSet = Set.copyOf(sanitizedUserAnswers);
                return !submittedSet.isEmpty() && submittedSet.equals(correctSet);
            case FILL_IN_BLANK:
                if (sanitizedUserAnswers.isEmpty()) {
                    return false;
                }
                String userAnswer = sanitizedUserAnswers.get(0);
                return correctAnswers.stream().anyMatch(ans -> ans.equalsIgnoreCase(userAnswer));
            default:
                return false;
        }
    }

    private String resolveDownloadPath(String downloadPath, boolean useDefaultPath) {
        if (useDefaultPath || !StringUtils.hasText(downloadPath)) {
            return defaultDownloadPath;
        }
        return downloadPath;
    }

    private void enforceDurationLimit(String videoUrl) {
        if (!enforceVideoLimit) {
            log.info("Video length enforcement disabled; skipping duration check for {}", videoUrl);
            return;
        }
        long duration = videoMetadataService.getDurationSeconds(videoUrl);
        if (duration <= 0) {
            throw new IllegalArgumentException("Unable to determine video length. Ensure yt-dlp is available and use videos up to " + (maxVideoSeconds / 60) + " minutes.");
        }
        if (duration > maxVideoSeconds) {
            throw new IllegalArgumentException("Video is longer than allowed limit of " + (maxVideoSeconds / 60) + " minutes.");
        }
    }

    private String fetchOrReuseTranscript(String videoUrl, String downloadPath) {
        Path directory = Path.of(downloadPath).toAbsolutePath();
        String cacheKey = resolveCacheKey(videoUrl);
        String fileName = "transcript-" + cacheKey + ".txt";
        Path transcriptFile = directory.resolve(fileName);
        try {
            Files.createDirectories(directory);
            if (Files.exists(transcriptFile)) {
                log.info("Reusing cached transcript for {} at {}", videoUrl, transcriptFile);
                return Files.readString(transcriptFile);
            }
            log.info("Transcript cache miss for {} (key={}), fetching...", videoUrl, cacheKey);
            String transcript = transcriptService.fetchTranscript(videoUrl);
            Files.writeString(transcriptFile, transcript);
            log.info("Transcript saved to {}", transcriptFile);
            return transcript;
        } catch (IOException e) {
            log.error("Failed to save transcript to {}", downloadPath, e);
            throw new IllegalStateException("Failed to save transcript to " + downloadPath, e);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }

    private String resolveCacheKey(String videoUrl) {
        String videoId = extractVideoId(videoUrl);
        if (StringUtils.hasText(videoId)) {
            return videoId;
        }
        return hash(videoUrl);
    }

    private String extractVideoId(String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return null;
        }
        // Patterns for common YouTube URLs
        Pattern watchPattern = Pattern.compile("[?&]v=([^&]+)");
        Matcher watchMatcher = watchPattern.matcher(videoUrl);
        if (watchMatcher.find()) {
            return watchMatcher.group(1);
        }
        Pattern shortPattern = Pattern.compile("youtu\\.be/([^?&/]+)");
        Matcher shortMatcher = shortPattern.matcher(videoUrl);
        if (shortMatcher.find()) {
            return shortMatcher.group(1);
        }
        Pattern embedPattern = Pattern.compile("/embed/([^?&/]+)");
        Matcher embedMatcher = embedPattern.matcher(videoUrl);
        if (embedMatcher.find()) {
            return embedMatcher.group(1);
        }
        return null;
    }
}
