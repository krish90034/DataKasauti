package com.datakasauti.engine.profile;

import com.datakasauti.engine.csv.ColumnType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProfileResult(
        long rowCount,
        int columnCount,
        long duplicateRows,
        long totalMissingValues,
        Map<String, Long> missingValues,
        List<ColumnSummary> columns,
        List<List<String>> previewRows
) {
    public ProfileResult {
        missingValues = Collections.unmodifiableMap(
                new LinkedHashMap<>(missingValues)
        );
        columns = List.copyOf(columns);
        previewRows = previewRows.stream()
                .map(row -> Collections.unmodifiableList(
                        new ArrayList<>(row)
                ))
                .toList();
    }

    public record ColumnSummary(
            String name,
            ColumnType detectedType,
            long missingCount,
            double missingPercent,
            long uniqueCount,
            NumericStats stats
    ) {
    }

    public record NumericStats(
            double min,
            double max,
            double mean,
            double median
    ) {
    }
}