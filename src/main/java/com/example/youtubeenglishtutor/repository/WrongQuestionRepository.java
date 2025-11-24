package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.WrongQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, Long> {
    void deleteByTestId(Long testId);
}
