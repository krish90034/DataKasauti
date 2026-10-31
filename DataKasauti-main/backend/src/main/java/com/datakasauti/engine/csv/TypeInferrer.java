package com.datakasauti.engine.csv;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Deterministic, single-column type inference. This class has no Spring dependency. */
public final class TypeInferrer {
    private TypeInferrer() {
    }

    public static ColumnType infer(List<String> values) {
        boolean allInteger = true;
        boolean allDecimal = true;
        boolean allBoolean = true;
        boolean allDate = true;
        long nonMissing = 0;

        for (String raw : values) {
            if (MissingValue.isMissing(raw)) {
                continue;
            }
            nonMissing++;
            String value = raw.strip();

            boolean integer = false;
            try {
                Long.parseLong(value);
                integer = true;
            } catch (NumberFormatException ignored) {
                // Continue with decimal detection.
            }
            if (!integer) {
                allInteger = false;
            }

            try {
                new BigDecimal(value);
            } catch (NumberFormatException ignored) {
                allDecimal = false;
            }

            if (!isBoolean(value)) {
                allBoolean = false;
            }
            if (!isDate(value)) {
                allDate = false;
            }
        }

        if (nonMissing == 0) return ColumnType.STRING;
        if (allInteger) return ColumnType.INTEGER;
        if (allDecimal) return ColumnType.DECIMAL;
        if (allBoolean) return ColumnType.BOOLEAN;
        if (allDate) return ColumnType.DATE;
        return ColumnType.STRING;
    }

    public static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    public static boolean isDate(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}
