package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.CatalogPreparation;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import com.example.youtubeenglishtutor.repository.CatalogPreparationRepository;
import com.example.youtubeenglishtutor.repository.CatalogVideoRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CatalogPackJob {

    private static final Logger log = LoggerFactory.getLogger(CatalogPackJob.class);

    private final CatalogVideoRepository catalogVideoRepository;
    private final CatalogPreparationRepository preparationRepository;
    private final CatalogPackService catalogPackService;

    @Value("${app.pregen.enabled:true}")
    private boolean enabled;

    @Value("${app.pregen.sizes:5,10,15}")
    private String sizesConfig;

    @Value("${app.pregen.nightly-cap:6}")
    private int nightlyCap;

    public CatalogPackJob(
            CatalogVideoRepository catalogVideoRepository,
            CatalogPreparationRepository preparationRepository,
            CatalogPackService catalogPackService) {
        this.catalogVideoRepository = catalogVideoRepository;
        this.preparationRepository = preparationRepository;
        this.catalogPackService = catalogPackService;
    }

    @Scheduled(cron = "${app.pregen.cron:0 10 3 * * *}")
    public void runNightly() {
        if (!enabled) {
            log.info("Pack pre-gen skipped: app.pregen.enabled=false");
            return;
        }
        List<Integer> sizes = parseSizes();
        List<CatalogVideo> candidates = catalogVideoRepository.findNeedingPrewarm(org.springframework.data.domain.PageRequest.of(0, nightlyCap)).getContent();
        if (candidates.isEmpty()) {
            log.info("Pack pre-gen: no candidates");
            return;
        }
        int processed = 0;
        for (CatalogVideo v : candidates) {
            if (processed >= nightlyCap) break;
            CatalogPreparation prep = preparationRepository.findByCatalogVideo(v).orElse(null);
            if (prep == null || !Boolean.TRUE.equals(prep.getTranscriptReady())) {
                continue;
            }
            for (int size : sizes) {
                try {
                    catalogPackService.generatePack(v, prep.getTranscript(), size, "NORMAL");
                } catch (Exception ignored) {
                }
            }
            processed++;
        }
        log.info("Pack pre-gen complete: attempted={} sizes={}", processed, sizes);
    }

    private List<Integer> parseSizes() {
        Set<Integer> parsed = Arrays.stream(sizesConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .filter(i -> i > 0)
                .collect(Collectors.toSet());
        if (parsed.isEmpty()) {
            return List.of(10);
        }
        return parsed.stream().sorted().toList();
    }
}

