 package com.datakasauti.dto.response;

public record FileSummaryDto(
        String filename,
        long rowCount,
        int columnCount
) {
}