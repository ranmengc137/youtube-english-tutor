package com.example.youtubeenglishtutor.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(value = "app.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

    private final RestClient restClient;
    private final String model;

    public OpenAiEmbeddingService(
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.embedding-model:text-embedding-3-small}") String model) {
        this.model = model;
        String resolvedKey = resolveApiKey(apiKey);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + resolvedKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<Double> embed(String text) {
        EmbeddingRequest request = new EmbeddingRequest(model, text);
        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);
        if (response == null || response.data == null || response.data.isEmpty()) {
            throw new IllegalStateException("Empty embedding response from OpenAI");
        }
        return response.data.get(0).embedding;
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
        throw new IllegalStateException("OpenAI API key missing.");
    }

    private record EmbeddingRequest(String model, String input) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EmbeddingResponse {
        public List<EmbeddingData> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EmbeddingData {
        public List<Double> embedding;
    }
}
