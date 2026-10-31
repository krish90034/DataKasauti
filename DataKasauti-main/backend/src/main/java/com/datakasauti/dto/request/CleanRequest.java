package com.datakasauti.dto.request;

import java.util.List;

public record CleanRequest(
        List<CleaningRuleDto> rules
) {
}
