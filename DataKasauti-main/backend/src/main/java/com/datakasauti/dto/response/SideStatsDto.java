package com.datakasauti.dto.response;

public record SideStatsDto(
        long count,
        long missing,
        double min,
        double max,
        double mean
) {
}