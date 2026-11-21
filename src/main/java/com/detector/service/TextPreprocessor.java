package com.detector.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class TextPreprocessor {
    private static final String[] STOP_WORDS = {
            "a", "an", "and", "are", "at", "be", "been", "but", "by", "for",
            "from", "has", "have", "in", "is", "it", "of", "on", "or", "our",
            "that", "the", "this", "to", "was", "we", "with", "you", "your"
    };

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("https?://", "")
                .replaceAll("[^a-z0-9]+", " ");
        List<String> stopWords = Arrays.asList(STOP_WORDS);
        return Arrays.stream(normalized.trim().split("\\s+"))
                .filter(token -> token.length() > 1)
                .filter(token -> !stopWords.contains(token))
                .collect(Collectors.toList());
    }
}
