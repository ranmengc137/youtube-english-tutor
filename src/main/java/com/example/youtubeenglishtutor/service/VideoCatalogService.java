package com.example.youtubeenglishtutor.service;

import com.example.youtubeenglishtutor.entity.CatalogCategory;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import com.example.youtubeenglishtutor.repository.CatalogVideoRepository;
import java.util.List;
import java.util.Random;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class VideoCatalogService {

    private final CatalogVideoRepository catalogVideoRepository;
    private final Random random = new Random();

    public VideoCatalogService(CatalogVideoRepository catalogVideoRepository) {
        this.catalogVideoRepository = catalogVideoRepository;
    }

    public CatalogVideo pickRandom(CatalogCategory category) {
        long count = catalogVideoRepository.countByCategoryAndActiveTrue(category);
        if (count <= 0) {
            return null;
        }
        int index = random.nextInt((int) Math.min(count, Integer.MAX_VALUE));
        Pageable page = PageRequest.of(index, 1, Sort.by(Sort.Direction.ASC, "id"));
        Page<CatalogVideo> result = catalogVideoRepository.findByCategoryAndActiveTrue(category, page);
        return result.hasContent() ? result.getContent().get(0) : null;
    }

    public CatalogVideo pickRandom(CatalogCategory category, long maxSeconds) {
        long count = catalogVideoRepository.countByCategoryAndActiveTrueAndDurationSecondsLessThanEqual(category, maxSeconds);
        if (count <= 0) {
            return null;
        }
        int index = random.nextInt((int) Math.min(count, Integer.MAX_VALUE));
        Pageable page = PageRequest.of(index, 1, Sort.by(Sort.Direction.ASC, "id"));
        Page<CatalogVideo> result = catalogVideoRepository.findByCategoryAndActiveTrueAndDurationSecondsLessThanEqual(category, maxSeconds, page);
        return result.hasContent() ? result.getContent().get(0) : null;
    }

    public Page<CatalogVideo> browse(
            CatalogCategory category,
            String difficulty,
            Long maxSeconds,
            String q,
            Pageable pageable) {
        String normalizedQ = (q != null && !q.isBlank()) ? "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%" : null;
        return catalogVideoRepository.browse(category, difficulty, maxSeconds, normalizedQ, pageable);
    }

    public Optional<CatalogVideo> findById(Long id) {
        return catalogVideoRepository.findById(id);
    }

    public List<CatalogCategory> categories() {
        return List.of(CatalogCategory.values());
    }
}
