package com.datakasauti.engine.clean;

import java.util.List;

public record CleaningRule(
        RuleType type,
        List<String> columns,
        String column,
        FillStrategy strategy,
        String value
) {
    public CleaningRule {
        columns = columns == null ? List.of() : List.copyOf(columns);
    }

    public static CleaningRule trimWhitespace() {
        return new CleaningRule(
                RuleType.TRIM_WHITESPACE,
                List.of(),
                null,
                null,
                null
        );
    }

    public static CleaningRule removeDuplicates() {
        return new CleaningRule(
                RuleType.REMOVE_DUPLICATES,
                List.of(),
                null,
                null,
                null
        );
    }

    public static CleaningRule dropRowsWithMissing(
            List<String> columns) {
        return new CleaningRule(
                RuleType.DROP_ROWS_WITH_MISSING,
                columns,
                null,
                null,
                null
        );
    }

    public static CleaningRule fillMissing(
            String column,
            FillStrategy strategy,
            String value) {
        return new CleaningRule(
                RuleType.FILL_MISSING,
                List.of(),
                column,
                strategy,
                value
        );
    }
}