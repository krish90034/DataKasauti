package com.datakasauti.dto.response;

public record RuleResultDto(
        String type,
        String column,
        long rowsAffected,
        long cellsChanged
) {
}