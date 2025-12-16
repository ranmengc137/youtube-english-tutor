package com.example.youtubeenglishtutor.repository;

import com.example.youtubeenglishtutor.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findTop20ByOrderByCreatedAtDesc();
}
