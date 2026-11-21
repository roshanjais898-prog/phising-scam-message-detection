package com.detector.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Component
public class TfIdfVectorizer {
    private final TextPreprocessor preprocessor;
    private final Map<String, Double> inverseDocumentFrequency = new HashMap<>();
    private final Set<String> vocabulary = new TreeSet<>();

    public TfIdfVectorizer(TextPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public void fit(List<String> documents) {
        inverseDocumentFrequency.clear();
        vocabulary.clear();

        List<List<String>> tokenized = documents.stream()
                .map(preprocessor::tokenize)
                .toList();

        Map<String, Integer> documentFrequency = new HashMap<>();
        for (List<String> tokens : tokenized) {
            vocabulary.addAll(tokens);
            Set<String> uniqueTokens = new TreeSet<>(tokens);
            for (String token : uniqueTokens) {
                documentFrequency.merge(token, 1, Integer::sum);
            }
        }

        int documentCount = documents.size();
        for (String term : vocabulary) {
            int df = documentFrequency.getOrDefault(term, 0);
            inverseDocumentFrequency.put(term,
                    Math.log((double) (documentCount + 1) / (df + 1)) + 1.0);
        }
    }

    public Map<String, Double> transform(String text) {
        if (vocabulary.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Double> termFrequency = new HashMap<>();
        for (String token : preprocessor.tokenize(text)) {
            termFrequency.merge(token, 1.0, Double::sum);
        }

        Map<String, Double> vector = new TreeMap<>();
        for (Map.Entry<String, Double> entry : termFrequency.entrySet()) {
            if (inverseDocumentFrequency.containsKey(entry.getKey())) {
                vector.put(entry.getKey(),
                        entry.getValue() * inverseDocumentFrequency.get(entry.getKey()));
            }
        }
        return vector;
    }

    public Set<String> getVocabulary() {
        return Collections.unmodifiableSet(vocabulary);
    }
}
