package com.example.youtubeenglishtutor.controller;

import com.example.youtubeenglishtutor.entity.CatalogCategory;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import com.example.youtubeenglishtutor.service.VideoCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StartInstantlyController {

    private static final Logger log = LoggerFactory.getLogger(StartInstantlyController.class);

    private final VideoCatalogService videoCatalogService;

    public StartInstantlyController(VideoCatalogService videoCatalogService) {
        this.videoCatalogService = videoCatalogService;
    }

    @GetMapping("/start-instantly")
    public String browse(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "q", required = false) String q,
            Model model) {

        CatalogCategory parsedCategory = parseCategory(category);
        Long maxSeconds = parseMaxSeconds(duration);
        String parsedDifficulty = (difficulty != null && !difficulty.isBlank() && !"ANY".equalsIgnoreCase(difficulty)) ? difficulty.trim() : null;

        Page<CatalogVideo> page = videoCatalogService.browse(
                parsedCategory,
                parsedDifficulty,
                maxSeconds,
                q,
                PageRequest.of(0, 36));

        log.info(
                "Browse catalog: category={} duration={} difficulty={} q='{}' results={}",
                parsedCategory != null ? parsedCategory.name() : "ANY",
                maxSeconds != null ? maxSeconds : "ANY",
                parsedDifficulty != null ? parsedDifficulty : "ANY",
                q != null ? q : "",
                page.getNumberOfElements());

        model.addAttribute("videos", page.getContent());
        model.addAttribute("categories", videoCatalogService.categories());
        model.addAttribute("selectedCategory", category != null ? category : "ANY");
        model.addAttribute("selectedDuration", duration != null ? duration : "ANY");
        model.addAttribute("selectedDifficulty", parsedDifficulty != null ? parsedDifficulty : "ANY");
        model.addAttribute("q", q != null ? q : "");
        return "start-instantly";
    }

    private CatalogCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank() || "ANY".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return CatalogCategory.valueOf(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseMaxSeconds(String duration) {
        if (duration == null || duration.isBlank() || "ANY".equalsIgnoreCase(duration)) {
            return null;
        }
        return switch (duration) {
            case "MICRO_8" -> 8L * 60L;
            case "MAX_15" -> 15L * 60L;
            case "MAX_30" -> 30L * 60L;
            default -> null;
        };
    }
}
