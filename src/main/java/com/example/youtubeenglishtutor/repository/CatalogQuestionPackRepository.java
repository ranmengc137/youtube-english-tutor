package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.CatalogQuestionPack;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogQuestionPackRepository extends JpaRepository<CatalogQuestionPack, Long> {

    Optional<CatalogQuestionPack> findFirstByCatalogVideoAndSize(CatalogVideo video, int size);

    List<CatalogQuestionPack> findByCatalogVideo(CatalogVideo video);

    @Query("""
            select p
            from CatalogQuestionPack p
            where p.catalogVideo = :video
            order by abs(p.size - :desiredSize) asc
            """)
    List<CatalogQuestionPack> findNearest(@Param("video") CatalogVideo video, @Param("desiredSize") int desiredSize);
}

