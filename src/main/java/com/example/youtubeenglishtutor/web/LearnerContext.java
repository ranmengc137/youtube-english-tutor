package com.example.youtubeenglishtutor.web;

import org.springframework.stereotype.Component;

/**
 * Holds the current anonymous learner id for the duration of a request.
 */
@Component
public class LearnerContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public void setCurrentLearnerId(String learnerId) {
        CURRENT.set(learnerId);
    }

    public String getCurrentLearnerId() {
        return CURRENT.get();
    }

    public void clear() {
        CURRENT.remove();
    }
}
