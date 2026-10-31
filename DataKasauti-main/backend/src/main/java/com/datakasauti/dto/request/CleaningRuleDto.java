package com.datakasauti.dto.request;

import java.util.List;

public record CleaningRuleDto(
        String type,
        String column,
        List<String> columns,
        String strategy,
        String value
) {
}