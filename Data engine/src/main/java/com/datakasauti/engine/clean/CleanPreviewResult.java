package com.datakasauti.engine.clean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CleanPreviewResult(
        List<String> columns,
        long rowsBefore,
        long rowsAfter,
        List<RuleResult> ruleResults,
        List<SampleChange> sampleChanges,
        List<RemovedRowSample> removedRowsSample,
        boolean originalModified,
        List<Map<String, String>> cleanedRows
) {
    public CleanPreviewResult {
        columns = List.copyOf(columns);
        ruleResults = List.copyOf(ruleResults);
        sampleChanges = List.copyOf(sampleChanges);
        removedRowsSample = List.copyOf(removedRowsSample);

        List<Map<String, String>> copiedRows = new ArrayList<>();

        for (Map<String, String> row : cleanedRows) {
            copiedRows.add(Collections.unmodifiableMap(
                    new LinkedHashMap<>(row)
            ));
        }

        cleanedRows = List.copyOf(copiedRows);
    }

    public record RuleResult(
            RuleType type,
            String column,
            long rowsAffected,
            long cellsChanged
    ) {
    }

    public record SampleChange(
            int rowNumber,
            String column,
            String before,
            String after
    ) {
    }

    public record RemovedRowSample(
            int rowNumber,
            RuleType rule,
            List<String> values
    ) {
        public RemovedRowSample {
            values = List.copyOf(values);
        }
    }
}