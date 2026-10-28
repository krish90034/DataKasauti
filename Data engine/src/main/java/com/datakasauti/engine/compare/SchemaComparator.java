package com.datakasauti.engine.compare;

import com.datakasauti.engine.csv.ColumnType;
import com.datakasauti.engine.profile.ProfileResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SchemaComparator {

    public CompareResult.SchemaResult compare(
            List<ProfileResult.ColumnSummary> baseline,
            List<ProfileResult.ColumnSummary> current) {

        Objects.requireNonNull(baseline, "baseline must not be null");
        Objects.requireNonNull(current, "current must not be null");

        Map<String, ColumnType> baselineTypes =
                getColumnTypes(baseline);

        Map<String, ColumnType> currentTypes =
                getColumnTypes(current);

        List<String> addedColumns = new ArrayList<>();
        List<String> removedColumns = new ArrayList<>();
        List<String> commonColumns = new ArrayList<>();

        List<CompareResult.TypeChange> typeChanges =
                new ArrayList<>();

        // Removed and common columns follow baseline order.
        for (Map.Entry<String, ColumnType> entry
                : baselineTypes.entrySet()) {

            String name = entry.getKey();
            ColumnType baselineType = entry.getValue();

            if (!currentTypes.containsKey(name)) {
                removedColumns.add(name);
                continue;
            }

            commonColumns.add(name);

            ColumnType currentType = currentTypes.get(name);

            if (baselineType != currentType) {
                typeChanges.add(
                        new CompareResult.TypeChange(
                                name,
                                baselineType,
                                currentType
                        )
                );
            }
        }

        // Added columns follow current dataset order.
        for (String name : currentTypes.keySet()) {
            if (!baselineTypes.containsKey(name)) {
                addedColumns.add(name);
            }
        }

        return new CompareResult.SchemaResult(
                addedColumns,
                removedColumns,
                commonColumns,
                typeChanges
        );
    }

    private Map<String, ColumnType> getColumnTypes(
            List<ProfileResult.ColumnSummary> columns) {

        Map<String, ColumnType> result = new LinkedHashMap<>();

        for (ProfileResult.ColumnSummary column : columns) {
            Objects.requireNonNull(column, "column must not be null");

            if (result.putIfAbsent(
                    column.name(),
                    column.detectedType()
            ) != null) {
                throw new IllegalArgumentException(
                        "Duplicate column name: " + column.name()
                );
            }
        }

        return result;
    }
}