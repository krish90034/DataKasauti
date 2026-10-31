package com.datakasauti.dto.response;

public record SampleChangeDto(
        long rowNumber,
        String column,
        String before,
        String after
) {
}