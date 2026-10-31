package com.datakasauti.dto.response;

import java.util.List;

public record RemovedRowDto(
        long rowNumber,
        String rule,
        List<String> values
) {
}