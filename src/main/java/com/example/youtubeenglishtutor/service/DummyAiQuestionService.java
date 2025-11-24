package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.Question;
import com.example.youtubeenglishtutor.entity.QuestionType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "app.ai.provider", havingValue = "dummy", matchIfMissing = true)
public class DummyAiQuestionService implements AiQuestionService {

    @Override
    public List<Question> generateQuestionsFromTranscript(String transcript) {
        List<Question> questions = new ArrayList<>();

        questions.add(singleChoice("What is the main topic discussed in the video?", "Travel tips;Cooking;Learning English;Technology", "Learning English"));
        questions.add(trueFalse("The speaker believes practice is important for learning.", "True"));
        questions.add(fillInBlank("According to the speaker, vocabulary should be practiced every ____.", "day"));
        questions.add(multipleChoice("Which skills are highlighted for improvement?", "Listening;Speaking;Dancing;Writing", "Listening;Speaking;Writing"));
        questions.add(singleChoice("What is recommended before watching a video?", "Turn off subtitles;Preview key vocabulary;Ignore the title;Skip the intro", "Preview key vocabulary"));
        questions.add(trueFalse("The video suggests taking notes while listening.", "True"));
        questions.add(fillInBlank("Repeating phrases out loud helps with _____ pronunciation.", "improving"));
        questions.add(singleChoice("How many practice questions are suggested per session?", "Five;Ten;Fifteen;Twenty", "Ten"));
        questions.add(multipleChoice("Which tools are mentioned for slowing down playback?", "YouTube speed control;Third-party apps;Physical notebook;Browser extensions", "YouTube speed control;Third-party apps;Browser extensions"));
        questions.add(trueFalse("The transcript mentions avoiding quizzes altogether.", "False"));

        return questions;
    }

    private Question singleChoice(String text, String options, String correct) {
        return buildQuestion(QuestionType.SINGLE_CHOICE, text, options, correct);
    }

    private Question multipleChoice(String text, String options, String correct) {
        return buildQuestion(QuestionType.MULTIPLE_CHOICE, text, options, correct);
    }

    private Question trueFalse(String text, String correct) {
        return buildQuestion(QuestionType.TRUE_FALSE, text, "True;False", correct);
    }

    private Question fillInBlank(String text, String correct) {
        return buildQuestion(QuestionType.FILL_IN_BLANK, text, "", correct);
    }

    private Question buildQuestion(QuestionType type, String text, String options, String correct) {
        Question question = new Question();
        question.setType(type);
        question.setText(text);
        question.setOptions(options);
        question.setCorrectAnswer(correct);
        return question;
    }
}
