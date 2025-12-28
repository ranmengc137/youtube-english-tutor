package com.example.youtubeenglishtutor.controller;

import com.example.youtubeenglishtutor.entity.CatalogCategory;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import com.example.youtubeenglishtutor.service.QuickStartPreparationService;
import com.example.youtubeenglishtutor.service.VideoCatalogService;
import com.example.youtubeenglishtutor.web.LearnerContext;
import com.example.youtubeenglishtutor.web.YoutubeUrlUtils;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class QuickStartController {

    private static final Logger log = LoggerFactory.getLogger(QuickStartController.class);

    private record QuickStartVideo(
            String videoUrl,
            String title,
            String category,
            String durationLabel
    ) {
    }

    private final QuickStartPreparationService preparationService;
    private final LearnerContext learnerContext;
    private final VideoCatalogService videoCatalogService;
    private final Random random = new Random();

    public QuickStartController(QuickStartPreparationService preparationService, LearnerContext learnerContext, VideoCatalogService videoCatalogService) {
        this.preparationService = preparationService;
        this.learnerContext = learnerContext;
        this.videoCatalogService = videoCatalogService;
    }

    @GetMapping("/watch/everyday")
    public String watchEveryday(Model model) {
        CatalogVideo picked = videoCatalogService.pickRandom(CatalogCategory.EVERYDAY_ENGLISH, 8 * 60);
        if (picked != null) {
            log.info("Quick start (EVERYDAY): picked from catalog pool videoId={} title='{}'", picked.getVideoId(), picked.getTitle());
            return renderWatch(new QuickStartVideo(
                    picked.getVideoUrl(),
                    picked.getTitle(),
                    "Everyday English",
                    "≤ 8 minutes"
            ), "Up next: quiz after video", model, 5);
        }
        log.info("Quick start (EVERYDAY): catalog pool empty, using fallback sample");
        QuickStartVideo fallback = pickOne(sampleEverydayEnglish());
        return renderWatch(fallback, "Up next: quiz after video", model, 5);
    }

    @GetMapping("/watch/ted")
    public String watchTed(Model model) {
        CatalogVideo picked = videoCatalogService.pickRandom(CatalogCategory.TED, 15 * 60);
        if (picked != null) {
            log.info("Quick start (TED): picked from catalog pool videoId={} title='{}'", picked.getVideoId(), picked.getTitle());
            return renderWatch(new QuickStartVideo(
                    picked.getVideoUrl(),
                    picked.getTitle(),
                    "TED",
                    "10–15 minutes"
            ), "Up next: quiz after video", model, 10);
        }
        log.info("Quick start (TED): catalog pool empty, using fallback sample");
        QuickStartVideo fallback = pickOne(sampleTed());
        return renderWatch(fallback, "Up next: quiz after video", model, 10);
    }

    @GetMapping("/watch")
    public String watchFromCatalog(@RequestParam("id") Long catalogId, Model model) {
        CatalogVideo picked = videoCatalogService.findById(catalogId).orElse(null);
        if (picked == null) {
            log.info("Watch from catalog: id={} not found; redirecting to /start-instantly", catalogId);
            return "redirect:/start-instantly";
        }
        log.info("Watch from catalog: id={} category={} videoId={} title='{}'", picked.getId(), picked.getCategory(), picked.getVideoId(), picked.getTitle());
        String categoryLabel = switch (picked.getCategory()) {
            case EVERYDAY_ENGLISH -> "Everyday English";
            case TED -> "TED";
            case BUSINESS_ENGLISH -> "Business English";
            case TECH_AI -> "Tech / AI";
            case TRAVEL_LIFESTYLE -> "Travel & Lifestyle";
        };
        String durationLabel = picked.getDurationSeconds() != null ? formatDuration(picked.getDurationSeconds()) : "≤ 30 minutes";
        return renderWatch(new QuickStartVideo(
                picked.getVideoUrl(),
                picked.getTitle(),
                categoryLabel,
                durationLabel
        ), "Up next: quiz after video", model, 10);
    }

    @GetMapping("/api/quick-start/prepare")
    @ResponseBody
    public Map<String, String> prepare(
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam(value = "count", required = false) Integer count) {
        String prepId = preparationService.start(learnerContext.getCurrentLearnerId(), videoUrl, count);
        return Map.of("prepId", prepId);
    }

    @GetMapping("/api/quick-start/status")
    @ResponseBody
    public QuickStartPreparationService.Status status(@RequestParam("prepId") String prepId) {
        return preparationService.status(prepId);
    }

    private QuickStartVideo pickOne(List<QuickStartVideo> list) {
        return list.get(random.nextInt(list.size()));
    }

    private String renderWatch(QuickStartVideo selected, String hint, Model model, int defaultQuestionCount) {
        model.addAttribute("videoUrl", selected.videoUrl());
        model.addAttribute("videoTitle", selected.title());
        model.addAttribute("videoCategory", selected.category());
        model.addAttribute("videoDuration", selected.durationLabel());
        model.addAttribute("videoId", YoutubeUrlUtils.extractVideoId(selected.videoUrl()));
        model.addAttribute("hint", hint);
        model.addAttribute("defaultQuestionCount", defaultQuestionCount);
        return "watch";
    }

    private List<QuickStartVideo> sampleEverydayEnglish() {
        return List.of(
                new QuickStartVideo(
                        "https://www.youtube.com/watch?v=H14bBuluwB8",
                        "Everyday English: small talk you can reuse today",
                        "Everyday English",
                        "≤ 8 minutes"
                ),
                new QuickStartVideo(
                        "https://www.youtube.com/watch?v=7T0d8yN-3yY",
                        "Everyday English: workplace conversation (short)",
                        "Everyday English",
                        "≤ 8 minutes"
                )
        );
    }

    private List<QuickStartVideo> sampleTed() {
        return List.of(
                new QuickStartVideo(
                        "https://www.youtube.com/watch?v=arj7oStGLkU",
                        "TED: How great leaders inspire action (short)",
                        "TED",
                        "10–15 minutes"
                ),
                new QuickStartVideo(
                        "https://www.youtube.com/watch?v=_gqsZ8bFrfI",
                        "TEDx: The art of concise communication (short)",
                        "TED",
                        "10–15 minutes"
                )
        );
    }

    private String formatDuration(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        if (mins <= 0) {
            return seconds + "s";
        }
        if (secs == 0) {
            return mins + "m";
        }
        return mins + "m " + secs + "s";
    }
}
