package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.CatalogQuestionPack;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import com.example.youtubeenglishtutor.entity.Question;
import com.example.youtubeenglishtutor.entity.QuestionType;
import com.example.youtubeenglishtutor.repository.CatalogQuestionPackRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CatalogPackService {

    private static final Logger log = LoggerFactory.getLogger(CatalogPackService.class);

    private final CatalogQuestionPackRepository packRepository;
    private final AiQuestionService aiQuestionService;
    private final ObjectMapper objectMapper;

    public CatalogPackService(CatalogQuestionPackRepository packRepository, AiQuestionService aiQuestionService, ObjectMapper objectMapper) {
        this.packRepository = packRepository;
        this.aiQuestionService = aiQuestionService;
        this.objectMapper = objectMapper;
    }

    public Optional<CatalogQuestionPack> findNearestPack(CatalogVideo video, int desiredSize) {
        List<CatalogQuestionPack> packs = packRepository.findNearest(video, desiredSize);
        return packs.stream().findFirst();
    }

    public CatalogQuestionPack generatePack(CatalogVideo video, String transcript, int size, String difficulty) {
        CatalogQuestionPack pack = packRepository.findFirstByCatalogVideoAndSize(video, size)
                .orElseGet(CatalogQuestionPack::new);
        pack.setCatalogVideo(video);
        pack.setSize(size);
        pack.setDifficulty(difficulty);
        pack.setIncludesWriting(true);
        try {
            List<Question> questions = aiQuestionService.generateQuestionsFromTranscript(
                    transcript,
                    DifficultyLevel.NORMAL,
                    size,
                    true);
            if (questions.stream().noneMatch(q -> q.getType() == QuestionType.WRITING)) {
                // ensure at least one writing question exists
                Question writing = new Question();
                writing.setType(QuestionType.WRITING);
                writing.setText("What is the main idea of this video?");
                writing.setCorrectAnswer("Summarize the central idea in 2-3 sentences.");
                questions.add(writing);
                questions = questions.stream()
                        .sorted(Comparator.comparingInt(q -> q.getType() == QuestionType.WRITING ? 1 : 0))
                        .limit(size)
                        .toList();
            }
            String json = objectMapper.writeValueAsString(questions);
            pack.setQuestionsJson(json);
            pack.setCreatedAt(LocalDateTime.now());
            pack.setLastError(null);
            packRepository.save(pack);
            log.info("Generated pack size={} for videoId={} (writing included)", size, video.getVideoId());
            return pack;
        } catch (Exception e) {
            pack.setLastError(e.getMessage());
            packRepository.save(pack);
            log.warn("Failed to generate pack size={} for videoId={} msg={}", size, video.getVideoId(), e.getMessage());
            throw new IllegalStateException("Failed to generate pack", e);
        }
    }

    public List<Question> materialize(CatalogQuestionPack pack) {
        try {
            return objectMapper.readValue(pack.getQuestionsJson(), new TypeReference<List<Question>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse question pack", e);
        }
    }
}

