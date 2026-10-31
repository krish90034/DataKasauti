package com.datakasauti.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * The single error response shape used by every API endpoint.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        List<String> details
) {
    public ApiErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
