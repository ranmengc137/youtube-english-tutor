package com.example.youtubeenglishtutor.controller;

import com.example.youtubeenglishtutor.entity.CatalogCategory;
import com.example.youtubeenglishtutor.service.VideoCatalogRefreshJob;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/catalog")
public class AdminCatalogController {

    private static final Logger log = LoggerFactory.getLogger(AdminCatalogController.class);

    private final VideoCatalogRefreshJob refreshJob;

    @Value("${app.admin.token:}")
    private String adminToken;

    public AdminCatalogController(VideoCatalogRefreshJob refreshJob) {
        this.refreshJob = refreshJob;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(value = "category", required = false, defaultValue = "ALL") String category) {
        if (StringUtils.hasText(adminToken) && !adminToken.equals(token)) {
            log.warn("Manual catalog refresh denied: missing/invalid X-Admin-Token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        log.info("Manual catalog refresh requested: category={}", category);
        if ("ALL".equalsIgnoreCase(category)) {
            Map<String, Object> stats = refreshJob.refreshAllWithStats();
            return ResponseEntity.ok(stats);
        }

        CatalogCategory parsed;
        try {
            parsed = CatalogCategory.valueOf(category.trim());
        } catch (Exception e) {
            String allowed = java.util.Arrays.stream(CatalogCategory.values())
                    .map(Enum::name)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid category. Use ALL or one of: " + allowed
            ));
        }

        Map<String, Object> stats = refreshJob.refreshCategoryWithStats(parsed);
        return ResponseEntity.ok(stats);
    }
}
