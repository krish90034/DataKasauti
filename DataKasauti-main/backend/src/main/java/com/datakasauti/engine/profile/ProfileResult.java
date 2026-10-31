package com.datakasauti.engine.profile;

import com.datakasauti.dto.response.ColumnProfileDto;
import com.datakasauti.dto.response.ProfileResponse;
import com.datakasauti.dto.response.WarningDto;
import java.util.List;
import java.util.Map;

/** Engine result for one profiled dataset; converted to the HTTP DTO at the service boundary. */
public record ProfileResult(
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
    public ProfileResult {
        missingValues = Map.copyOf(missingValues);
        columns = List.copyOf(columns);
        previewRows = previewRows.stream().map(List::copyOf).toList();
        warnings = List.copyOf(warnings);
    }

    public ProfileResponse toResponse() {
        return new ProfileResponse(filename, fileSizeBytes, rowCount, columnCount,
                duplicateRows, totalMissingValues, missingValues, columns, previewRows, warnings);
    }
}
