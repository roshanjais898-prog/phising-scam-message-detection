package com.detector.service;

import com.detector.model.TrainingDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NaiveBayesClassifier {
    private static final double ALPHA = 0.5;
    private final TextPreprocessor preprocessor;
    private final TfIdfVectorizer vectorizer;
    private final Map<String, Integer> documentCounts = new HashMap<>();
    private final Map<String, Map<String, Double>> featureTotals = new HashMap<>();
    private final Map<String, Double> featureSums = new HashMap<>();
    private int documentCount;

    public NaiveBayesClassifier(TextPreprocessor preprocessor, TfIdfVectorizer vectorizer) {
        this.preprocessor = preprocessor;
        this.vectorizer = vectorizer;
    }

    public void train(List<TrainingDocument> documents) {
        documentCounts.clear();
        featureTotals.clear();
        featureSums.clear();

        List<String> texts = new ArrayList<>();
        for (TrainingDocument document : documents) {
            texts.add(document.text());
            documentCounts.merge(document.label(), 1, Integer::sum);
            featureTotals.putIfAbsent(document.label(), new HashMap<>());
        }
        documentCount = documents.size();
        vectorizer.fit(texts);

        for (TrainingDocument document : documents) {
            Map<String, Double> totals = featureTotals.get(document.label());
            double classSum = 0.0;
            for (Map.Entry<String, Double> feature : vectorizer.transform(document.text()).entrySet()) {
                totals.merge(feature.getKey(), feature.getValue(), Double::sum);
                classSum += feature.getValue();
            }
            featureSums.merge(document.label(), classSum, Double::sum);
        }
    }

    public String predict(String text) {
        Map<String, Double> tfIdf = vectorizer.transform(text);
        String bestLabel = "SUSPICIOUS";
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Map.Entry<String, Integer> entry : documentCounts.entrySet()) {
            String label = entry.getKey();
            double score = Math.log(entry.getValue() / (double) documentCount);
            Map<String, Double> totals = featureTotals.get(label);
            double denominator = featureSums.getOrDefault(label, 0.0)
                    + ALPHA * vectorizer.getVocabulary().size();

            for (Map.Entry<String, Double> feature : tfIdf.entrySet()) {
                double numerator = totals.getOrDefault(feature.getKey(), 0.0) + ALPHA;
                score += feature.getValue() * Math.log(numerator / denominator);
            }
            if (score > bestScore) {
                bestScore = score;
                bestLabel = label;
            }
        }
        return bestLabel;
    }
}
