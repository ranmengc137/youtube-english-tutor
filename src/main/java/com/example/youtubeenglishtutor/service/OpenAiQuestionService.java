package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.Question;
import com.example.youtubeenglishtutor.entity.QuestionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiQuestionService implements AiQuestionService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiQuestionService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiQuestionService(
            ObjectMapper objectMapper,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-3.5-turbo}") String model) {
        this.objectMapper = objectMapper;
        this.model = model;
        String resolvedKey = resolveApiKey(apiKey);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + resolvedKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<Question> generateQuestionsFromTranscript(String transcript, DifficultyLevel difficulty) {
        OpenAiChatRequest request = buildRequest(transcript, difficulty);
        log.info("Calling OpenAI model={} for quiz generation", model);
        OpenAiChatResponse response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(OpenAiChatResponse.class);

        if (response == null || response.choices == null || response.choices.isEmpty()) {
            log.warn("OpenAI returned empty response");
            return Collections.emptyList();
        }

        String content = response.choices.get(0).message.content;
        log.debug("OpenAI raw content: {}", content);
        return parseQuestionsFromContent(content);
    }

    private OpenAiChatRequest buildRequest(String transcript, DifficultyLevel difficulty) {
        String systemPrompt = """
                You create English quiz questions from transcripts. Produce EXACTLY 10 questions as a JSON array.
                Each item: {"type":"SINGLE_CHOICE|MULTIPLE_CHOICE|TRUE_FALSE|FILL_IN_BLANK","text":"...","options":["opt1","opt2"],"correct":["answer1","answer2"]}.
                For TRUE_FALSE use options ["True","False"]. For FILL_IN_BLANK, options can be empty, but correct must have one answer.
                Only return the JSON array, nothing else.
                """;
        String difficultyHint = difficulty != null ? difficulty.toPromptTag() : DifficultyLevel.NORMAL.toPromptTag();

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", difficultyHint + "\nTranscript:\n" + transcript)
        );
        return new OpenAiChatRequest(model, messages, 0.3);
    }

    private List<Question> parseQuestionsFromContent(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            if (root == null || !root.isArray()) {
                throw new IllegalStateException("Expected JSON array of questions");
            }
            List<Question> questions = new ArrayList<>();
            for (JsonNode node : root) {
                questions.add(toQuestion(node));
            }
            log.info("Parsed {} questions from OpenAI", questions.size());
            return questions;
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response", e);
            throw new IllegalStateException("Failed to parse OpenAI response: " + content, e);
        }
    }

    private Question toQuestion(JsonNode node) {
        String typeRaw = node.path("type").asText();
        QuestionType type = QuestionType.valueOf(typeRaw.toUpperCase(Locale.ROOT));
        Question question = new Question();
        question.setType(type);
        question.setText(node.path("text").asText());

        List<String> options = extractList(node.get("options"));
        if (!options.isEmpty()) {
            question.setOptions(String.join(";", options));
        }

        List<String> correct = extractList(node.get("correct"));
        if (!correct.isEmpty()) {
            question.setCorrectAnswer(String.join(";", correct));
        }
        return question;
    }

    private List<String> extractList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        }
        if (node.isTextual()) {
            String text = node.asText();
            if (!StringUtils.hasText(text)) {
                return List.of();
            }
            // Split semicolon or comma separated strings if they look combined.
            if (text.contains(";")) {
                return Arrays.stream(text.split(";"))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList();
            }
            if (text.contains(",")) {
                return Arrays.stream(text.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList();
            }
            return List.of(text);
        }
        return List.of(node.asText());
    }

    private String resolveApiKey(String apiKey) {
        if (StringUtils.hasText(apiKey)) {
            return apiKey;
        }
        String fromEnv = System.getenv("OPENAI_API_KEY");
        if (StringUtils.hasText(fromEnv)) {
            return fromEnv;
        }
        log.error("OpenAI API key missing; set app.openai.api-key or OPENAI_API_KEY");
        throw new IllegalStateException("OpenAI API key missing. Set app.openai.api-key or OPENAI_API_KEY env var.");
    }

    private record ChatMessage(String role, String content) {
    }

    private record OpenAiChatRequest(
            String model,
            List<ChatMessage> messages,
            @JsonProperty("temperature") double temperature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenAiChatResponse {
        public List<Choice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Choice {
        public Message message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Message {
        public String content;
    }

    private static class ParsedQuestion {
        public String type;
        public String text;
        public List<String> options;
        public List<String> correct;
    }
}
