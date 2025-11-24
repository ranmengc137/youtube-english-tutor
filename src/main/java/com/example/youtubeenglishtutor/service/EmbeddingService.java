package com.example.youtubeenglishtutor.service;

import java.util.List;

public interface EmbeddingService {
    List<Double> embed(String text);
}
