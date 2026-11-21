package com.detector.service;

import com.detector.model.DetectionResponse;
import com.detector.model.TrainingDocument;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessageDetectionService {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://)?(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,}|(?:\\d{1,3}\\.){3}\\d{1,3})(?:/[\\w\\-./?%&=+#]*)?");

    private static final Set<String> SCAM_KEYWORDS = Set.of(
            "winner", "won", "prize", "lottery", "claim", "cash reward",
            "urgent", "password", "pin", "otp", "bank account", "credit card",
            "send money", "wire transfer", "gift card", "bitcoin", "crypto",
            "inheritance", "refund", "tax", "penalty", "suspended",
            "verification", "verify", "account locked", "social security",
            "irs", "police", "arrest", "bail", "romance", "investment",
            "guaranteed", "double your", "free money"
    );

    private static final Set<String> SUSPICIOUS_KEYWORDS = Set.of(
            "urgent", "act now", "limited time", "click", "link",
            "login", "signin", "confirm", "update", "payment",
            "delivery", "package", "tracking", "fee", "expired"
    );

    private static final Set<String> SHORTENERS = Set.of(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly"
    );

    private final NaiveBayesClassifier classifier;

    public MessageDetectionService(NaiveBayesClassifier classifier) {
        this.classifier = classifier;
    }

    @PostConstruct
    public void trainModel() {
        List<TrainingDocument> trainingData = List.of(
                new TrainingDocument("meeting scheduled tomorrow in conference room", "SAFE"),
                new TrainingDocument("your appointment is confirmed for monday", "SAFE"),
                new TrainingDocument("dinner is at seven please let me know", "SAFE"),
                new TrainingDocument("project report is attached for your review", "SAFE"),
                new TrainingDocument("family gathering starts this weekend", "SAFE"),
                new TrainingDocument("thank you for your order it will arrive soon", "SAFE"),
                new TrainingDocument("your package will be delivered tomorrow", "SAFE"),
                new TrainingDocument("team meeting has been moved to friday", "SAFE"),
                new TrainingDocument("please review the document before lunch", "SAFE"),
                new TrainingDocument("happy birthday hope you have a great day", "SAFE"),
                new TrainingDocument("pay your invoice through the official billing portal", "SUSPICIOUS"),
                new TrainingDocument("unusual login detected confirm activity", "SUSPICIOUS"),
                new TrainingDocument("delivery needs address confirmation before shipment", "SUSPICIOUS"),
                new TrainingDocument("update payment details to continue service", "SUSPICIOUS"),
                new TrainingDocument("account requires verification please use official link", "SUSPICIOUS"),
                new TrainingDocument("limited time offer click the link now", "SUSPICIOUS"),
                new TrainingDocument("package tracking link has an additional fee", "SUSPICIOUS"),
                new TrainingDocument("your password will expire update it today", "SUSPICIOUS"),
                new TrainingDocument("security alert review login from new device", "SUSPICIOUS"),
                new TrainingDocument("invoice payment is overdue act immediately", "SUSPICIOUS"),
                new TrainingDocument("congratulations winner claim your cash prize now", "SCAM"),
                new TrainingDocument("you won lottery inheritance send bank details", "SCAM"),
                new TrainingDocument("urgent IRS penalty pay bitcoin gift cards", "SCAM"),
                new TrainingDocument("send otp pin credit card or account password", "SCAM"),
                new TrainingDocument("police arrest warrant requires immediate bail wire transfer", "SCAM"),
                new TrainingDocument("guaranteed investment double money send crypto", "SCAM"),
                new TrainingDocument("free refund claim verify social security number", "SCAM"),
                new TrainingDocument("account suspended transfer money to unlock", "SCAM"),
                new TrainingDocument("romance partner needs gift card bitcoin", "SCAM"),
                new TrainingDocument("tax refund click link and enter bank account", "SCAM")
        );
        classifier.train(trainingData);
    }

    public DetectionResponse detect(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        List<String> indicators = new ArrayList<>();
        List<String> links = extractLinks(message == null ? "" : message);

        int scamRulePoints = 0;
        int suspiciousRulePoints = 0;

        for (String keyword : SCAM_KEYWORDS) {
            if (containsPhrase(normalized, keyword)) {
                indicators.add("Scam keyword: \"" + keyword + "\"");
                scamRulePoints += 2;
            }
        }
        for (String keyword : SUSPICIOUS_KEYWORDS) {
            if (containsPhrase(normalized, keyword) && !indicators.stream()
                    .anyMatch(item -> item.contains(keyword))) {
                indicators.add("Suspicious keyword: \"" + keyword + "\"");
                suspiciousRulePoints++;
            }
        }

        for (String link : links) {
            String lowerLink = link.toLowerCase(Locale.ROOT);
            if (lowerLink.startsWith("http://")) {
                indicators.add("Link does not use HTTPS");
                suspiciousRulePoints += 2;
            }
            if (SHORTENERS.stream().anyMatch(lowerLink::contains)) {
                indicators.add("Shortened or masked link");
                suspiciousRulePoints += 2;
            }
            if (lowerLink.matches(".*(?:\\d{1,3}\\.){3}\\d{1,3}.*")) {
                indicators.add("Link uses an IP address instead of a domain");
                scamRulePoints += 2;
            }
            if (lowerLink.matches(".*\\.(xyz|top|buzz|ru|cn|tk|ml|ga|cf)(?:[/:].*)?$")) {
                indicators.add("Link uses a commonly abused domain");
                suspiciousRulePoints += 2;
            }
        }

        if (links.size() > 1) {
            indicators.add("Message contains multiple links");
            suspiciousRulePoints++;
        }
        if (normalized.matches(".*\\b(?:pay|send|transfer|enter|provide|confirm)\\b.*")
                && normalized.matches(".*\\b(?:otp|pin|password|bank|money|card|bitcoin|crypto)\\b.*")) {
            indicators.add("Requests sensitive information or payment");
            scamRulePoints += 3;
        }

        String mlLabel = classifier.predict(message);
        int mlPoints = switch (mlLabel) {
            case "SCAM" -> 3;
            case "SUSPICIOUS" -> 1;
            default -> 0;
        };

        int ruleRiskPoints = scamRulePoints + suspiciousRulePoints;
        int totalRiskPoints = ruleRiskPoints + (ruleRiskPoints > 0 ? mlPoints : 0);
        String classification;
        if (scamRulePoints >= 5
                || (mlLabel.equals("SCAM") && scamRulePoints >= 3)
                || (scamRulePoints >= 3 && suspiciousRulePoints >= 4)) {
            classification = "SCAM";
        } else if (ruleRiskPoints >= 1 || !links.isEmpty()) {
            classification = "SUSPICIOUS";
        } else {
            classification = "SAFE";
        }

        double riskScore = Math.min(100.0, (classification.equals("SAFE") ? 0.0
                : classification.equals("SUSPICIOUS") ? 35.0 : 75.0)
                + Math.min(25.0, totalRiskPoints * 4.0));

        String explanation = switch (classification) {
            case "SCAM" -> "High-risk language, suspicious payment requests, or deceptive links were detected.";
            case "SUSPICIOUS" -> "This message has warning signs but does not show enough evidence for a definite scam.";
            default -> "No strong scam indicators were detected.";
        };

        if (indicators.isEmpty()) {
            indicators.add("No known scam indicators detected");
        }
        return new DetectionResponse(classification, Math.round(riskScore * 10.0) / 10.0,
                indicators, links, explanation);
    }

    private List<String> extractLinks(String message) {
        LinkedHashSet<String> links = new LinkedHashSet<>();
        Matcher matcher = URL_PATTERN.matcher(message);
        while (matcher.find()) {
            links.add(matcher.group().replaceAll("[),.;]+$", ""));
        }
        return new ArrayList<>(links);
    }

    private boolean containsPhrase(String normalizedMessage, String phrase) {
        StringBuilder expression = new StringBuilder("(?<![a-z0-9])");
        String[] words = phrase.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                expression.append("\\s+");
            }
            expression.append(Pattern.quote(words[i]));
        }
        expression.append("(?![a-z0-9])");
        String pattern = expression.toString();
        return Pattern.compile(pattern).matcher(normalizedMessage).find();
    }
}
