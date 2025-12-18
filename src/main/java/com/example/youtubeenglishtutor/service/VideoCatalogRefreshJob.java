package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.CatalogCategory;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import com.example.youtubeenglishtutor.repository.CatalogVideoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Manual trigger
 *
 *   - Endpoint: POST /admin/catalog/refresh
 *   - Optional params:
 *       - category=ALL (default) or a single category enum (e.g. EVERYDAY_ENGLISH,
 *         TED)
 *   - Optional header guard:
 *       - If you set app.admin.token, you must pass X-Admin-Token: <token>
 *       - If app.admin.token is empty/unset, the endpoint is open (dev-friendly).
 *
 *   File: src/main/java/com/example/youtubeenglishtutor/controller/
 *   AdminCatalogController.java
 *
 *   Examples
 *
 *   - Refresh everything:
 *       - curl -X POST http://localhost:8080/admin/catalog/refresh
 *   - Refresh one category:
 *       - curl -X POST "http://localhost:8080/admin/catalog/refresh?category=TED"
 *   - With token:
 *       - curl -X POST http://localhost:8080/admin/catalog/refresh -H "X-Admin-Token: YOUR_TOKEN"
 */
@Service
public class VideoCatalogRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(VideoCatalogRefreshJob.class);

    public record RefreshStats(
            int target,
            int candidates,
            int saved,
            int deactivated,
            int skippedDuration,
            int skippedNoCaptions,
            int skippedMissingTitle,
            int skippedMissingFields
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("target", target);
            map.put("candidates", candidates);
            map.put("saved", saved);
            map.put("deactivated", deactivated);
            map.put("skippedDuration", skippedDuration);
            map.put("skippedNoCaptions", skippedNoCaptions);
            map.put("skippedMissingTitle", skippedMissingTitle);
            map.put("skippedMissingFields", skippedMissingFields);
            return map;
        }
    }

    private final YouTubeDataApiClient youTubeDataApiClient;
    private final CatalogVideoRepository catalogVideoRepository;

    @Value("${app.catalog.pool-size:100}")
    private int targetPoolSize;

    @Value("${app.catalog.refresh.enabled:true}")
    private boolean refreshEnabled;

    public VideoCatalogRefreshJob(YouTubeDataApiClient youTubeDataApiClient, CatalogVideoRepository catalogVideoRepository) {
        this.youTubeDataApiClient = youTubeDataApiClient;
        this.catalogVideoRepository = catalogVideoRepository;
    }

    @Scheduled(cron = "${app.catalog.refresh-cron:0 0 */6 * * *}")
    public void scheduledRefresh() {
        if (!refreshEnabled) {
            log.info("Catalog refresh skipped: app.catalog.refresh.enabled=false");
            return;
        }
        if (!youTubeDataApiClient.isConfigured()) {
            log.info("Catalog refresh skipped: app.youtube.api-key not configured.");
            return;
        }
        try {
            Map<String, Object> stats = refreshAllWithStats();
            log.info("Catalog refresh finished: {}", stats);
        } catch (Exception e) {
            log.warn("Catalog refresh failed: {}", e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> refreshAllWithStats() {
        long started = System.currentTimeMillis();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("startedAt", LocalDateTime.now().toString());

        Map<String, Object> categories = new LinkedHashMap<>();
        categories.put(CatalogCategory.EVERYDAY_ENGLISH.name(),
                refreshCategory(CatalogCategory.EVERYDAY_ENGLISH, "everyday English conversation", 8 * 60, "short", "closedCaption", "Easy").toMap());
        categories.put(CatalogCategory.TED.name(),
                refreshCategory(CatalogCategory.TED, "TED talk English", 30 * 60, "medium", "closedCaption", "Medium").toMap());
        categories.put(CatalogCategory.BUSINESS_ENGLISH.name(),
                refreshCategory(CatalogCategory.BUSINESS_ENGLISH, "business English meeting phrases", 30 * 60, "medium", "closedCaption", "Medium").toMap());
        categories.put(CatalogCategory.TECH_AI.name(),
                refreshCategory(CatalogCategory.TECH_AI, "AI explained in English", 30 * 60, "medium", "closedCaption", "Medium").toMap());
        categories.put(CatalogCategory.TRAVEL_LIFESTYLE.name(),
                refreshCategory(CatalogCategory.TRAVEL_LIFESTYLE, "travel English phrases", 30 * 60, "medium", "closedCaption", "Easy").toMap());

        out.put("categories", categories);
        out.put("tookMs", System.currentTimeMillis() - started);
        return out;
    }

    @Transactional
    public Map<String, Object> refreshCategoryWithStats(CatalogCategory category) {
        long started = System.currentTimeMillis();
        RefreshStats stats = switch (category) {
            case EVERYDAY_ENGLISH -> refreshCategory(category, "everyday English conversation", 8 * 60, "short", "closedCaption", "Easy");
            case TED -> refreshCategory(category, "TED talk English", 30 * 60, "medium", "closedCaption", "Medium");
            case BUSINESS_ENGLISH -> refreshCategory(category, "business English meeting phrases", 30 * 60, "medium", "closedCaption", "Medium");
            case TECH_AI -> refreshCategory(category, "AI explained in English", 30 * 60, "medium", "closedCaption", "Medium");
            case TRAVEL_LIFESTYLE -> refreshCategory(category, "travel English phrases", 30 * 60, "medium", "closedCaption", "Easy");
        };
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("category", category.name());
        out.putAll(stats.toMap());
        out.put("tookMs", System.currentTimeMillis() - started);
        return out;
    }

    private RefreshStats refreshCategory(
            CatalogCategory category,
            String query,
            long maxSeconds,
            String videoDuration,
            String videoCaption,
            String defaultDifficulty) {
        long started = System.currentTimeMillis();
        int desired = Math.max(50, Math.min(targetPoolSize, 200));
        Set<String> candidates = new HashSet<>();
        String pageToken = null;
        int guard = 0;
        while (candidates.size() < desired && guard < 10) {
            guard++;
            YouTubeDataApiClient.SearchPage page = youTubeDataApiClient.searchVideos(query, pageToken, 50, videoDuration, videoCaption);
            candidates.addAll(page.videoIds());
            pageToken = page.nextPageToken();
            if (!StringUtils.hasText(pageToken)) {
                break;
            }
        }

        List<String> candidateList = new ArrayList<>(candidates);
        int saved = 0;
        int skippedDuration = 0;
        int skippedNoCaptions = 0;
        int skippedMissingTitle = 0;
        int skippedMissingFields = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < candidateList.size(); i += 50) {
            List<String> batch = candidateList.subList(i, Math.min(i + 50, candidateList.size()));
            JsonNode details = youTubeDataApiClient.videosDetails(batch);
            if (details == null || !details.has("items")) {
                continue;
            }
            for (JsonNode item : details.get("items")) {
                String videoId = item.hasNonNull("id") ? item.get("id").asText() : null;
                if (!StringUtils.hasText(videoId)) {
                    skippedMissingFields++;
                    continue;
                }
                JsonNode contentDetails = item.get("contentDetails");
                JsonNode snippet = item.get("snippet");
                if (contentDetails == null || snippet == null) {
                    skippedMissingFields++;
                    continue;
                }
                String isoDuration = contentDetails.hasNonNull("duration") ? contentDetails.get("duration").asText() : null;
                long seconds = YoutubeDurationParser.parseSeconds(isoDuration);
                if (seconds <= 0 || seconds > maxSeconds) {
                    skippedDuration++;
                    continue;
                }
                boolean captions = contentDetails.hasNonNull("caption") && "true".equalsIgnoreCase(contentDetails.get("caption").asText());
                if (!captions) {
                    skippedNoCaptions++;
                    continue;
                }
                String title = snippet.hasNonNull("title") ? snippet.get("title").asText() : null;
                if (!StringUtils.hasText(title)) {
                    skippedMissingTitle++;
                    continue;
                }
                String channelTitle = snippet.hasNonNull("channelTitle") ? snippet.get("channelTitle").asText() : null;
                String thumb = pickThumbnail(snippet.get("thumbnails"));
                String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

                CatalogVideo record = catalogVideoRepository
                        .findFirstByCategoryAndVideoId(category, videoId)
                        .orElseGet(CatalogVideo::new);
                record.setCategory(category);
                record.setVideoId(videoId);
                record.setVideoUrl(videoUrl);
                record.setTitle(title);
                record.setChannelTitle(channelTitle);
                record.setThumbnailUrl(thumb);
                record.setDurationSeconds(seconds);
                record.setCaptionsAvailable(true);
                record.setDifficulty(defaultDifficulty);
                record.setActive(true);
                record.setRefreshedAt(now);
                record.setLastSeenAt(now);
                record.setSourceQuery(query);
                catalogVideoRepository.save(record);
                saved++;
                if (saved >= desired) {
                    break;
                }
            }
            if (saved >= desired) {
                break;
            }
        }

        // Deactivate stale entries that haven't appeared in the search set recently.
        LocalDateTime cutoff = now.minusDays(21);
        int deactivated = catalogVideoRepository.deactivateStale(category, cutoff);
        long tookMs = System.currentTimeMillis() - started;
        log.info(
                "Catalog refresh: category={} query='{}' target={} candidates={} saved={} deactivated={} skipped(duration/noCaptions/missingTitle/missingFields)={}/{}/{}/{} took={}s",
                category,
                query,
                desired,
                candidates.size(),
                saved,
                deactivated,
                skippedDuration,
                skippedNoCaptions,
                skippedMissingTitle,
                skippedMissingFields,
                Duration.ofMillis(tookMs).toSeconds());

        return new RefreshStats(
                desired,
                candidates.size(),
                saved,
                deactivated,
                skippedDuration,
                skippedNoCaptions,
                skippedMissingTitle,
                skippedMissingFields);
    }

    private String pickThumbnail(JsonNode thumbnails) {
        if (thumbnails == null) {
            return null;
        }
        // prefer high/medium/default
        String[] keys = {"high", "medium", "default"};
        for (String key : keys) {
            JsonNode node = thumbnails.get(key);
            if (node != null && node.hasNonNull("url")) {
                return node.get("url").asText();
            }
        }
        return null;
    }
}
