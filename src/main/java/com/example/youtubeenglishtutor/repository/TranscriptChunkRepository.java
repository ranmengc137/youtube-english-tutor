package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.TranscriptChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranscriptChunkRepository extends JpaRepository<TranscriptChunk, Long> {
    List<TranscriptChunk> findByTestId(Long testId);
    void deleteByTestId(Long testId);
}
