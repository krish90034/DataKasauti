package com.datakasauti.dto.response;

public record CommonColumnDto(
        String name,
        SideStatsDto baseline,
        SideStatsDto current,
        double meanDelta
) {
}