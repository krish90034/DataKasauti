package com.datakasauti.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthResponse(
        String status,
        String version,

        @JsonProperty("max_upload_mb")
        int maxUploadMb
) {
}