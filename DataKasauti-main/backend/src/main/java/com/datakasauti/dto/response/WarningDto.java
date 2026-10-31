 package com.datakasauti.dto.response;

public record WarningDto(
        String code,
        String severity,
        String message
) {
}