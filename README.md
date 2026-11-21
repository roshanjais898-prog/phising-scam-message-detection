# Phishing and Scam Message Detector

A complete Java 17 and Spring Boot application that classifies text messages as **Safe**, **Suspicious**, or **Scam**. It combines a TF-IDF/Naive Bayes classifier with rule-based detection for scam keywords, suspicious links, insecure URLs, shortened URLs, and sensitive-information requests.

## Features

- Attractive responsive browser interface
- Spring Boot REST API
- Java TF-IDF vectorization
- Java multinomial Naive Bayes classifier
- Keyword and suspicious-link heuristics
- Safe, Suspicious, and Scam result levels
- Risk score and detected-indicator details
- Unit and MockMvc tests
- Sample test cases

## Technology

- Java 17+
- Spring Boot 3.4
- Maven
- HTML, CSS, and JavaScript
- JUnit 5 and Spring Boot Test

## Project Structure

```text
src/main/java/com/detector
├── PhishingDetectorApplication.java
├── controller
│   ├── ApiExceptionHandler.java
│   └── DetectionController.java
├── model
│   ├── DetectionRequest.java
│   ├── DetectionResponse.java
│   └── TrainingDocument.java
└── service
    ├── TextPreprocessor.java
    ├── TfIdfVectorizer.java
    ├── NaiveBayesClassifier.java
    └── MessageDetectionService.java

src/main/resources/static
├── index.html
├── styles.css
└── app.js

src/test/java/com/detector
├── controller
│   └── DetectionControllerTest.java
└── service
    └── MessageDetectionServiceTest.java

src/test/resources
└── sample-test-cases.csv
```

## Run Locally

Requirements:

- JDK 17 or newer
- Maven 3.9+ (or the Maven wrapper, if added later)

Build and run:

```bash
mvn clean spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

## API

Analyze a message:

```http
POST /api/detect
Content-Type: application/json

{
  "message": "Congratulations! You won a prize. Send your OTP now."
}
```

Example response:

```json
{
  "classification": "SCAM",
  "riskScore": 100.0,
  "detectedIndicators": [
    "Scam keyword: \"winner\"",
    "Scam keyword: \"prize\""
  ],
  "suspiciousLinks": [],
  "explanation": "High-risk language, suspicious payment requests, or deceptive links were detected."
}
```

Blank or messages over 5000 characters are rejected with HTTP 400.

## Tests

Run the complete test suite:

```bash
mvn test
```

Representative sample cases are in `src/test/resources/sample-test-cases.csv`.

## Detection Method

The application trains the TF-IDF and Naive Bayes model in memory at startup from a small built-in labeled corpus. Text is normalized and tokenized, TF-IDF vectors are calculated, and multinomial Naive Bayes predicts a Safe, Suspicious, or Scam label. Rule-based checks add risk for sensitive terms, payment requests, HTTP links, IP addresses, shortened URLs, and commonly abused domains.

This project is intended as a demonstration and safety aid, not a replacement for official fraud prevention or security software.
