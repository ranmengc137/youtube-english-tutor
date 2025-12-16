package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.Test;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    Optional<Test> findFirstByVideoUrlAndLearnerIdOrderByCreatedAtDesc(String videoUrl, String learnerId);
    Optional<Test> findByIdAndLearnerId(Long id, String learnerId);
    List<Test> findByLearnerIdOrderByCreatedAtDesc(String learnerId);
}
