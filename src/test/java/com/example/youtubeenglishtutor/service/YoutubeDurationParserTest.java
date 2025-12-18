package com.example.youtubeenglishtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class YoutubeDurationParserTest {

    @Test
    void parsesIsoDurationSeconds() {
        assertEquals(0, YoutubeDurationParser.parseSeconds("PT0S"));
        assertEquals(59, YoutubeDurationParser.parseSeconds("PT59S"));
        assertEquals(60, YoutubeDurationParser.parseSeconds("PT1M"));
        assertEquals(90, YoutubeDurationParser.parseSeconds("PT1M30S"));
        assertEquals(3600, YoutubeDurationParser.parseSeconds("PT1H"));
        assertEquals(3661, YoutubeDurationParser.parseSeconds("PT1H1M1S"));
    }

    @Test
    void returnsMinusOneOnBadInput() {
        assertEquals(-1, YoutubeDurationParser.parseSeconds(null));
        assertEquals(-1, YoutubeDurationParser.parseSeconds(""));
        assertEquals(-1, YoutubeDurationParser.parseSeconds("n/a"));
    }
}

