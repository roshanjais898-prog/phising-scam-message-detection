package com.detector.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DetectionRequest {
    @NotBlank(message = "Message text is required")
    @Size(max = 5000, message = "Message text must not exceed 5000 characters")
    private String message;

    public DetectionRequest() {
    }

    public DetectionRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
