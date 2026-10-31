package com.datakasauti.controller;

import com.datakasauti.config.UploadProperties;
import com.datakasauti.dto.response.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final UploadProperties uploadProperties;

    public HealthController(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @GetMapping("/health")
    public HealthResponse getHealth() {
        return new HealthResponse(
                "UP",
                "1.0.0",
                uploadProperties.getMaxUploadMb()
        );
    }
}