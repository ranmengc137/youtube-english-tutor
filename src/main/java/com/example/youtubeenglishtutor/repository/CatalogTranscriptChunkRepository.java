package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.CatalogTranscriptChunk;
import com.example.youtubeenglishtutor.entity.CatalogVideo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CatalogTranscriptChunkRepository extends JpaRepository<CatalogTranscriptChunk, Long> {

    List<CatalogTranscriptChunk> findByCatalogVideo(CatalogVideo catalogVideo);

    @Transactional
    void deleteByCatalogVideo(CatalogVideo catalogVideo);
}

