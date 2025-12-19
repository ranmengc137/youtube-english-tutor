package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.Question;
import com.example.youtubeenglishtutor.entity.QuestionType;
import com.example.youtubeenglishtutor.entity.Test;
import com.example.youtubeenglishtutor.entity.WrongQuestion;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import com.example.youtubeenglishtutor.entity.CatalogPreparation;
import com.example.youtubeenglishtutor.entity.CatalogTranscriptChunk;
import com.example.youtubeenglishtutor.entity.CatalogQuestionPack;
import com.example.youtubeenglishtutor.service.ObservabilityService;
import com.example.youtubeenglishtutor.service.CatalogPackService;
import com.example.youtubeenglishtutor.repository.TestRepository;
import com.example.youtubeenglishtutor.repository.TranscriptChunkRepository;
import com.example.youtubeenglishtutor.repository.WrongQuestionRepository;
import com.example.youtubeenglishtutor.repository.CatalogVideoRepository;
import com.example.youtubeenglishtutor.repository.CatalogPreparationRepository;
import com.example.youtubeenglishtutor.repository.CatalogTranscriptChunkRepository;
import com.example.youtubeenglishtutor.repository.CatalogQuestionPackRepository;
import com.example.youtubeenglishtutor.web.LearnerContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final CatalogVideoRepository catalogVideoRepository;
    private final CatalogPreparationRepository catalogPreparationRepository;
    private final CatalogTranscriptChunkRepository catalogTranscriptChunkRepository;
    private final CatalogQuestionPackRepository catalogQuestionPackRepository;
    private final CatalogPackService catalogPackService;
    private final VideoMetadataService videoMetadataService;
    private final ObservabilityService observabilityService;
    private final LearnerContext learnerContext;
    private final ConcurrentHashMap<String, Object> transcriptLocks = new ConcurrentHashMap<>();

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
            CatalogVideoRepository catalogVideoRepository,
            CatalogPreparationRepository catalogPreparationRepository,
            CatalogTranscriptChunkRepository catalogTranscriptChunkRepository,
            CatalogQuestionPackRepository catalogQuestionPackRepository,
            CatalogPackService catalogPackService,
            VideoMetadataService videoMetadataService,
            ObservabilityService observabilityService,
            LearnerContext learnerContext) {
        this.testRepository = testRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.aiQuestionService = aiQuestionService;
        this.transcriptService = transcriptService;
        this.ragService = ragService;
        this.chunkRepository = chunkRepository;
        this.catalogVideoRepository = catalogVideoRepository;
        this.catalogPreparationRepository = catalogPreparationRepository;
        this.catalogTranscriptChunkRepository = catalogTranscriptChunkRepository;
        this.catalogQuestionPackRepository = catalogQuestionPackRepository;
        this.catalogPackService = catalogPackService;
        this.videoMetadataService = videoMetadataService;
        this.observabilityService = observabilityService;
        this.learnerContext = learnerContext;
    }

    public Test createTest(String videoUrl, String downloadPath, boolean useDefaultPath) {
        return createTest(videoUrl, downloadPath, useDefaultPath, null);
    }

    @Transactional
    public Test createTest(String videoUrl, String downloadPath, boolean useDefaultPath, Integer desiredSize) {
        String learnerId = learnerContext.getCurrentLearnerId();
        String resolvedPath = resolveDownloadPath(downloadPath, useDefaultPath);
        log.info("Creating test for videoUrl={} using downloadPath={}", videoUrl, resolvedPath);
        // Optional<Test> existing = testRepository.findFirstByVideoUrlAndLearnerIdOrderByCreatedAtDesc(videoUrl, learnerId);
        // if (existing.isPresent()) {
        //     log.info("Reusing existing test {} for videoUrl={}", existing.get().getId(), videoUrl);
        //     return existing.get();
        // }
        enforceDurationLimit(videoUrl);
        String videoTitle = resolveTitle(videoUrl);

        CatalogPreparation prewarm = null;
        CatalogVideo catalogVideo = null;
        String videoId = com.example.youtubeenglishtutor.web.YoutubeUrlUtils.extractVideoId(videoUrl);
        if (StringUtils.hasText(videoId)) {
            catalogVideo = catalogVideoRepository.findFirstByVideoId(videoId).orElse(null);
            if (catalogVideo != null) {
                prewarm = catalogPreparationRepository.findByCatalogVideo(catalogVideo).orElse(null);
            }
        }

        String transcript = (prewarm != null && Boolean.TRUE.equals(prewarm.getTranscriptReady()) && StringUtils.hasText(prewarm.getTranscript()))
                ? prewarm.getTranscript()
                : fetchOrReuseTranscript(videoUrl, resolvedPath);
        log.debug("Transcript ready ({} chars)", transcript != null ? transcript.length() : 0);

        Test test = new Test();
        test.setLearnerId(learnerId);
        test.setVideoUrl(videoUrl);
        test.setVideoTitle(videoTitle);
        test.setTranscript(transcript);

        int targetSize = desiredSize != null && desiredSize > 0 ? desiredSize : 10;
        List<Question> generatedQuestions = null;
        if (catalogVideo != null) {
            Optional<CatalogQuestionPack> packOpt = catalogPackService.findNearestPack(catalogVideo, targetSize);
            if (packOpt.isPresent()) {
                List<Question> packQuestions = catalogPackService.materialize(packOpt.get());
                packQuestions.forEach(test::addQuestion);
                test.setTotalQuestions(packQuestions.size());
                test = testRepository.save(test);
                log.info("Used pre-generated pack size={} (actual={}) for videoId={}", targetSize, packQuestions.size(), videoId);
            }
        }

        if (test.getId() == null) {
            generatedQuestions = aiQuestionService.generateQuestionsFromTranscript(transcript, DifficultyLevel.NORMAL, targetSize, false);
            generatedQuestions.forEach(test::addQuestion);
            test.setTotalQuestions(generatedQuestions.size());
            test = testRepository.save(test);
        }
        final String videoIdFinal = videoId;
        final Test savedTest = test;
        if (prewarm != null && Boolean.TRUE.equals(prewarm.getEmbeddingsReady())) {
            List<CatalogTranscriptChunk> catalogChunks = catalogTranscriptChunkRepository.findByCatalogVideo(catalogVideo);
            if (!catalogChunks.isEmpty()) {
                chunkRepository.deleteByTestId(test.getId());
                List<com.example.youtubeenglishtutor.entity.TranscriptChunk> chunks = catalogChunks.stream().map(cc -> {
                    com.example.youtubeenglishtutor.entity.TranscriptChunk c = new com.example.youtubeenglishtutor.entity.TranscriptChunk();
                    c.setTest(savedTest);
                    c.setContent(cc.getContent());
                    c.setEmbedding(cc.getEmbedding());
                    return c;
                }).toList();
                chunkRepository.saveAll(chunks);
                log.info("Reused prewarmed embeddings for videoId={} chunks={}", videoIdFinal, catalogChunks.size());
            } else {
                ragService.saveChunks(test, transcript, chunkSize, chunkOverlap);
            }
        } else {
            ragService.saveChunks(test, transcript, chunkSize, chunkOverlap);
        }

        return testRepository.save(test);
    }

    private String resolveTitle(String videoUrl) {
        String title = videoMetadataService.getTitle(videoUrl);
        if (!StringUtils.hasText(title)) {
            return "YouTube Video";
        }
        return title;
    }

    @Transactional(readOnly = true)
    public List<Test> listTests() {
        String learnerId = learnerContext.getCurrentLearnerId();
        if (!StringUtils.hasText(learnerId)) {
            log.warn("listTests: learnerId missing on request");
            return List.of();
        }
        return testRepository.findByLearnerIdOrderByCreatedAtDesc(learnerId);
    }

    @Transactional(readOnly = true)
    public Test getTest(Long id) {
        String learnerId = learnerContext.getCurrentLearnerId();
        if (!StringUtils.hasText(learnerId)) {
            log.warn("getTest: learnerId missing on request id={}", id);
            return null;
        }
        return testRepository.findByIdAndLearnerId(id, learnerId).orElse(null);
    }

    @Transactional
    public Test submitAnswers(Long testId, Map<Long, List<String>> answersByQuestionId) {
        String learnerId = learnerContext.getCurrentLearnerId();
        Optional<Test> testOpt = testRepository.findByIdAndLearnerId(testId, learnerId);
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
            // Log judge outcome per question for observability.
            observabilityService.logJudgeEvent(
                    learnerId,
                    testId,
                    question.getId(),
                    correct ? "CORRECT" : "INCORRECT",
                    String.join(";", submittedAnswers));
        }

        test.setScore(score);
        test.setTotalQuestions(questions.size());
        return testRepository.save(test);
    }

    @Transactional
    public Test regenerateTest(Long testId, DifficultyLevel difficulty) {
        String learnerId = learnerContext.getCurrentLearnerId();
        Test test = testRepository.findByIdAndLearnerId(testId, learnerId).orElse(null);
        if (test == null) {
            log.warn("regenerateTest: test not found id={}", testId);
            return null;
        }
        if (!StringUtils.hasText(test.getTranscript())) {
            throw new IllegalStateException("Transcript missing; cannot regenerate. Please recreate the test.");
        }

        wrongQuestionRepository.deleteByTestId(testId);
        test.getWrongQuestions().clear();
        test.getQuestions().clear();
        test.setScore(null);
        test.setTotalQuestions(null);

        List<Question> regenerated = aiQuestionService.generateQuestionsFromTranscript(test.getTranscript(), difficulty);
        regenerated.forEach(test::addQuestion);
        test.setTotalQuestions(regenerated.size());
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
            case WRITING:
                // Writing is open-ended; treat as correct for scoring. Feedback happens elsewhere.
                return true;
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
            throw new IllegalArgumentException("Unable to determine video length. Ensure yt-dlp is available and use videos up to " + (maxVideoSeconds / 60) + " minutes. Check that yt-dlp is on PATH and returns --get-duration output without errors.");
        }
        if (duration > maxVideoSeconds) {
            throw new IllegalArgumentException("Video is longer than allowed limit of " + (maxVideoSeconds / 60) + " minutes.");
        }
    }

    private String fetchOrReuseTranscript(String videoUrl, String downloadPath) {
        String cacheKey = resolveCacheKey(videoUrl);
        Object lock = transcriptLocks.computeIfAbsent(cacheKey, k -> new Object());

        synchronized (lock) {
            Path directory = Path.of(downloadPath).toAbsolutePath();
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
        String videoId = com.example.youtubeenglishtutor.web.YoutubeUrlUtils.extractVideoId(videoUrl);
        if (StringUtils.hasText(videoId)) {
            return videoId;
        }
        return hash(videoUrl);
    }
}
