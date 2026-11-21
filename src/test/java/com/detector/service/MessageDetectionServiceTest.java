package com.detector.service;

import com.detector.model.DetectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageDetectionServiceTest {
    private MessageDetectionService service;

    @BeforeEach
    void setUp() {
        TextPreprocessor preprocessor = new TextPreprocessor();
        TfIdfVectorizer vectorizer = new TfIdfVectorizer(preprocessor);
        NaiveBayesClassifier classifier = new NaiveBayesClassifier(preprocessor, vectorizer);
        service = new MessageDetectionService(classifier);
        service.trainModel();
    }

    @ParameterizedTest
    @CsvSource({
            "'Your appointment is confirmed for Monday','SAFE'",
            "'Hi, are you still coming to dinner tonight?','SAFE'",
            "'The team meeting starts at 10 AM in room 4','SAFE'",
            "'Urgent: confirm your account login at login.example.com','SUSPICIOUS'",
            "'Your package has a delivery fee, track it at http://tracking.xyz','SUSPICIOUS'",
            "'Congratulations winner, claim your cash prize by sending an OTP','SCAM'",
            "'The IRS says you owe tax; pay immediately with gift cards','SCAM'",
            "'Send your bank account and password to collect your lottery winnings','SCAM'"
    })
    void detectsRepresentativeMessages(String message, String expected) {
        DetectionResponse response = service.detect(message);
        assertEquals(expected, response.classification());
    }

    @Test
    void detectsInsecureAndShortenedLinks() {
        DetectionResponse response = service.detect(
                "Please verify immediately: http://bit.ly/verify-account");

        assertTrue(response.suspiciousLinks().size() >= 1);
        assertTrue(response.detectedIndicators().stream()
                .anyMatch(indicator -> indicator.contains("Shortened")
                        || indicator.contains("HTTPS")));
    }

    @Test
    void safeResponseIncludesZeroOrLowRiskScore() {
        DetectionResponse response = service.detect("Happy birthday! Have a great day.");

        assertEquals("SAFE", response.classification());
        assertTrue(response.riskScore() < 50.0);
    }

    @Test
    void responseAlwaysContainsAnExplanationAndIndicators() {
        DetectionResponse response = service.detect("The project report is ready.");

        assertFalse(response.explanation().isBlank());
        assertFalse(response.detectedIndicators().isEmpty());
    }

    @Test
    void doesNotMatchShortKeywordsInsideLargerWords() {
        DetectionResponse response = service.detect(
                "I brought a pineapple and paid for it with a taxi.");

        assertEquals("SAFE", response.classification());
    }

    @Test
    void detectsIpAddressLink() {
        DetectionResponse response = service.detect(
                "Login immediately at http://192.168.1.100/account");

        assertTrue(response.suspiciousLinks().contains("http://192.168.1.100/account"));
        assertTrue(response.detectedIndicators().stream()
                .anyMatch(indicator -> indicator.contains("IP address")));
    }
}
