package com.detector.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DetectionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsDetectionResult() throws Exception {
        mockMvc.perform(post("/api/detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Congratulations, you won a lottery. Send your OTP now.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classification").exists())
                .andExpect(jsonPath("$.riskScore").isNumber())
                .andExpect(jsonPath("$.detectedIndicators").isArray());
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
