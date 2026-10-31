package com.datakasauti.engine.compare;

import com.datakasauti.dto.response.SchemaChangeDto;
import com.datakasauti.dto.response.WarningDto;
import com.datakasauti.engine.csv.ColumnType;
import com.datakasauti.engine.csv.CsvData;
import java.util.ArrayList;
import java.util.List;

/** Compares exact, case-sensitive CSV column names and their inferred types. */
public final class SchemaComparator {
    public SchemaComparison compare(CsvData baseline, CsvData current) {
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<SchemaChangeDto> changes = new ArrayList<>();
        List<WarningDto> warnings = new ArrayList<>();

        for (String column : current.headers()) {
            if (!baseline.headers().contains(column)) {
                added.add(column);
                warnings.add(new WarningDto("COLUMN_ADDED", "LOW",
                        "Column " + column + " exists in current only"));
            }
        }
        for (String column : baseline.headers()) {
            if (!current.headers().contains(column)) {
                removed.add(column);
                warnings.add(new WarningDto("COLUMN_REMOVED", "HIGH",
                        "Column " + column + " exists in baseline only"));
            }
        }
        for (String column : baseline.headers()) {
            if (!current.headers().contains(column)) continue;
            ColumnType left = baseline.typeOf(column);
            ColumnType right = current.typeOf(column);
            if (left != right) {
                changes.add(new SchemaChangeDto(column, left.name(), right.name()));
                warnings.add(new WarningDto("TYPE_CHANGED", "HIGH",
                        "Column " + column + " changed type from " + left + " to " + right));
            }
        }
        return new SchemaComparison(List.copyOf(added), List.copyOf(removed),
                List.copyOf(changes), List.copyOf(warnings));
    }
}
