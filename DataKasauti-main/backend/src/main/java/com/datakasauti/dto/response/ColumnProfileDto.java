 package com.datakasauti.dto.response;

public record ColumnProfileDto(
        String name,
        String detectedType,
        long missingCount,
        double missingPercent,
        long uniqueCount,
        ColumnStatsDto stats
) {
}