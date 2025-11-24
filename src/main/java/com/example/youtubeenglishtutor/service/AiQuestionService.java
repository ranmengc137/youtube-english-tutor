package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.Question;
import java.util.List;

public interface AiQuestionService {
    List<Question> generateQuestionsFromTranscript(String transcript);
}
