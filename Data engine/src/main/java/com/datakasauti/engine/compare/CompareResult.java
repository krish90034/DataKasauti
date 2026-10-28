package com.datakasauti.engine.compare;

import com.datakasauti.engine.csv.ColumnType;
import java.util.List;

public record CompareResult(
        long baselineRowCount,
        long currentRowCount,
        List<String> addedColumns,
        List<String> removedColumns,
        List<String> commonColumns,
        List<TypeChange> typeChanges,
        List<CommonColumnStats> commonColumnStats,
        RowChanges rowChanges,
        List<String> warnings
) {
    public CompareResult {
        addedColumns = List.copyOf(addedColumns);
        removedColumns = List.copyOf(removedColumns);
        commonColumns = List.copyOf(commonColumns);
        typeChanges = List.copyOf(typeChanges);
        commonColumnStats = List.copyOf(commonColumnStats);
        warnings = List.copyOf(warnings);
    }

    public long rowCountDelta() {
        return currentRowCount - baselineRowCount;
    }

    public record TypeChange(
            String column,
            ColumnType baselineType,
            ColumnType currentType
    ) {}

    public record SideStats(
            long count,
            long missing,
            double min,
            double max,
            double mean
    ) {}

    public record CommonColumnStats(
            String name,
            SideStats baseline,
            SideStats current,
            double meanDelta
    ) {}

    public record RowChanges(
            String keyColumn,
            long addedKeysCount,
            long removedKeysCount,
            long unchangedKeysCount,
            long changedKeysCount
    ) {}

    public record SchemaResult(
            List<String> addedColumns,
            List<String> removedColumns,
            List<String> commonColumns,
            List<TypeChange> typeChanges
    ) {
        public SchemaResult {
            addedColumns = List.copyOf(addedColumns);
            removedColumns = List.copyOf(removedColumns);
            commonColumns = List.copyOf(commonColumns);
            typeChanges = List.copyOf(typeChanges);
        }
    }
}