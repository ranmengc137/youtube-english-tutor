package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.CatalogCategory;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogVideoRepository extends JpaRepository<CatalogVideo, Long> {

    Optional<CatalogVideo> findFirstByCategoryAndVideoId(CatalogCategory category, String videoId);

    long countByCategoryAndActiveTrue(CatalogCategory category);

    Page<CatalogVideo> findByCategoryAndActiveTrue(CatalogCategory category, Pageable pageable);

    long countByCategoryAndActiveTrueAndDurationSecondsLessThanEqual(CatalogCategory category, long maxSeconds);

    Page<CatalogVideo> findByCategoryAndActiveTrueAndDurationSecondsLessThanEqual(CatalogCategory category, long maxSeconds, Pageable pageable);

    @Query("""
            select v
            from CatalogVideo v
            where v.active = true
              and (:category is null or v.category = :category)
              and (:difficulty is null or v.difficulty = :difficulty)
              and (:maxSeconds is null or v.durationSeconds <= :maxSeconds)
              and (:q is null or lower(v.title) like lower(concat('%', :q, '%')))
            order by v.refreshedAt desc nulls last, v.createdAt desc
            """)
    Page<CatalogVideo> browse(
            @Param("category") CatalogCategory category,
            @Param("difficulty") String difficulty,
            @Param("maxSeconds") Long maxSeconds,
            @Param("q") String q,
            Pageable pageable);

    @Modifying
    @Query("""
            update CatalogVideo v
            set v.active = false
            where v.category = :category
              and v.lastSeenAt is not null
              and v.lastSeenAt < :cutoff
            """)
    int deactivateStale(@Param("category") CatalogCategory category, @Param("cutoff") LocalDateTime cutoff);
}
