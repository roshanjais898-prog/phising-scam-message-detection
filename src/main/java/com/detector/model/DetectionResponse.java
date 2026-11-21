package com.detector.model;

import java.util.List;

public record DetectionResponse(
        String classification,
        double riskScore,
        List<String> detectedIndicators,
        List<String> suspiciousLinks,
        String explanation
) {
}
