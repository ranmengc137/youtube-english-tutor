package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.CatalogVideo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CatalogPrewarmJob {

    private static final Logger log = LoggerFactory.getLogger(CatalogPrewarmJob.class);

    private final CatalogPrewarmService prewarmService;

    @Value("${app.prewarm.enabled:true}")
    private boolean enabled;

    @Value("${app.prewarm.nightly-cap:10}")
    private int nightlyCap;

    public CatalogPrewarmJob(CatalogPrewarmService prewarmService) {
        this.prewarmService = prewarmService;
    }

    @Scheduled(cron = "${app.prewarm.cron:0 30 2 * * *}")
    public void runNightly() {
        if (!enabled) {
            log.info("Catalog prewarm skipped: app.prewarm.enabled=false");
            return;
        }
        List<CatalogVideo> candidates = prewarmService.findNeedingPrewarm(nightlyCap);
        if (candidates.isEmpty()) {
            log.info("Catalog prewarm: no candidates needing work");
            return;
        }
        log.info("Catalog prewarm: processing {} candidate(s)", candidates.size());
        for (CatalogVideo v : candidates) {
            prewarmService.prewarm(v);
        }
    }
}

