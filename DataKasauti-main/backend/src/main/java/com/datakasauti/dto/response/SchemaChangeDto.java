package com.datakasauti.dto.response;

public record SchemaChangeDto(
        String column,
        String baselineType,
        String currentType
) {
}