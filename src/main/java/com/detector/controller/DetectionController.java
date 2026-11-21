package com.detector.controller;

import com.detector.model.DetectionRequest;
import com.detector.model.DetectionResponse;
import com.detector.service.MessageDetectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DetectionController {
    private final MessageDetectionService detectionService;

    public DetectionController(MessageDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @PostMapping("/detect")
    public ResponseEntity<DetectionResponse> detect(@Valid @RequestBody DetectionRequest request) {
        return ResponseEntity.ok(detectionService.detect(request.getMessage()));
    }
}
