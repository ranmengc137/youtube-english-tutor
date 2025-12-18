package com.example.youtubeenglishtutor.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

@Service
public class YouTubeDataApiClient {

    private static final Logger log = LoggerFactory.getLogger(YouTubeDataApiClient.class);

    private final RestTemplate restTemplate;

    @Value("${app.youtube.api-key:}")
    private String apiKey;

    public YouTubeDataApiClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    public SearchPage searchVideos(
            String query,
            String pageToken,
            int maxResults,
            String videoDuration,
            String videoCaption) {
        log.debug("YouTube API search: q='{}' duration={} caption={} pageToken={}", query, videoDuration, videoCaption, pageToken);
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/youtube/v3/search")
                .queryParam("part", "snippet")
                .queryParam("type", "video")
                .queryParam("q", query)
                .queryParam("maxResults", Math.min(maxResults, 50))
                .queryParam("key", apiKey)
                .queryParam("relevanceLanguage", "en")
                .queryParam("hl", "en")
                .queryParam("safeSearch", "strict")
                .queryParamIfPresent("videoDuration", opt(videoDuration))
                .queryParamIfPresent("videoCaption", opt(videoCaption))
                .queryParamIfPresent("pageToken", opt(pageToken))
                .encode()
                .build()
                .toUri();

        ResponseEntity<JsonNode> resp = restTemplate.getForEntity(uri, JsonNode.class);
        JsonNode body = resp.getBody();
        if (body == null) {
            return new SearchPage(List.of(), null);
        }
        String next = body.hasNonNull("nextPageToken") ? body.get("nextPageToken").asText(null) : null;
        List<String> ids = new ArrayList<>();
        JsonNode items = body.get("items");
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                JsonNode id = item.get("id");
                if (id != null && id.hasNonNull("videoId")) {
                    ids.add(id.get("videoId").asText());
                }
            }
        }
        return new SearchPage(ids, next);
    }

    public JsonNode videosDetails(List<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return null;
        }
        log.debug("YouTube API videos.list: ids={}", videoIds.size());
        String joined = String.join(",", videoIds.stream().limit(50).toList());
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/youtube/v3/videos")
                .queryParam("part", "contentDetails,snippet")
                .queryParam("id", joined)
                .queryParam("key", apiKey)
                .encode()
                .build()
                .toUri();

        ResponseEntity<JsonNode> resp = restTemplate.getForEntity(uri, JsonNode.class);
        return resp.getBody();
    }

    private static java.util.Optional<String> opt(String value) {
        return StringUtils.hasText(value) ? java.util.Optional.of(value) : java.util.Optional.empty();
    }

    public record SearchPage(List<String> videoIds, String nextPageToken) {
    }
}
