package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.Question;
import java.util.List;

public interface AiQuestionService {
    default List<Question> generateQuestionsFromTranscript(String transcript) {
        return generateQuestionsFromTranscript(transcript, DifficultyLevel.NORMAL);
    }

    List<Question> generateQuestionsFromTranscript(String transcript, DifficultyLevel difficulty);
}
