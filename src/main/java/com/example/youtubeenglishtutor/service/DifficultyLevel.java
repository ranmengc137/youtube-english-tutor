package com.example.youtubeenglishtutor.service;

public enum DifficultyLevel {
    EASIER,
    NORMAL,
    HARDER;

    public String toPromptTag() {
        return switch (this) {
            case EASIER -> "Make questions simpler and more direct. Prefer TRUE_FALSE and SINGLE_CHOICE with clear cues.";
            case HARDER -> "Increase difficulty. Use MULTIPLE_CHOICE and FILL_IN_BLANK with distractors and less obvious cues.";
            default -> "Keep a balanced difficulty.";
        };
    }
}
