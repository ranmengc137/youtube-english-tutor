package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.CatalogPreparation;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogPreparationRepository extends JpaRepository<CatalogPreparation, Long> {
    Optional<CatalogPreparation> findByCatalogVideo(CatalogVideo catalogVideo);
}

