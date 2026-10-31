package com.datakasauti.dto.response;

public record ColumnStatsDto(
        double min,
        double max,
        double mean,
        double median
) {
}