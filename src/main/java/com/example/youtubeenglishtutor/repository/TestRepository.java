package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
}
