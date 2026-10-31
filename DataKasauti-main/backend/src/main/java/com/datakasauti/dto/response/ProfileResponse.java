package com.datakasauti.dto.response;

import java.util.List;
import java.util.Map;

public record ProfileResponse(
        String filename,
        long fileSizeBytes,
        long rowCount,
        int columnCount,
        long duplicateRows,
        long totalMissingValues,
        Map<String, Long> missingValues,
        List<ColumnProfileDto> columns,
        List<List<String>> previewRows,
        List<WarningDto> warnings
) {
}