package com.example.youtubeenglishtutor.controller;

import com.example.youtubeenglishtutor.service.MetricsSnapshot;
import com.example.youtubeenglishtutor.service.ObservabilityService;
import com.example.youtubeenglishtutor.service.MetricsExportService;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.StringUtils;

@Controller
@RequestMapping("/admin/metrics")
public class AdminMetricsController {

    private final ObservabilityService observabilityService;
    private final MetricsExportService metricsExportService;

    public AdminMetricsController(ObservabilityService observabilityService, MetricsExportService metricsExportService) {
        this.observabilityService = observabilityService;
        this.metricsExportService = metricsExportService;
    }

    @GetMapping
    public String metrics(Model model) {
        MetricsSnapshot snapshot = observabilityService.buildMetricsSnapshot(7);
        model.addAttribute("metrics", snapshot);
        return "admin-metrics";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(value = "day", required = false) String day) throws IOException {
        LocalDate targetDay = StringUtils.hasText(day) ? LocalDate.parse(day) : LocalDate.now();
        var file = metricsExportService.exportForDate(targetDay);
        byte[] bytes = Files.readAllBytes(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFileName())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
