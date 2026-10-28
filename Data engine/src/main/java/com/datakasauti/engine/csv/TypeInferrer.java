package com.datakasauti.engine.csv;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class TypeInferrer {

    private static final Pattern ISO_DATE_PATTERN =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private TypeInferrer() {
    }

    public static ColumnType infer(List<String> values) {
        Objects.requireNonNull(values, "values must not be null");

        List<String> presentValues = new ArrayList<>();

        for (String value : values) {
            if (!MissingValue.isMissing(value)) {
                presentValues.add(value.trim());
            }
        }

        if (presentValues.isEmpty()) {
            return ColumnType.STRING;
        }

        if (allMatch(presentValues, TypeInferrer::isInteger)) {
            return ColumnType.INTEGER;
        }

        if (allMatch(presentValues, TypeInferrer::isDecimal)) {
            return ColumnType.DECIMAL;
        }

        if (allMatch(presentValues, TypeInferrer::isBoolean)) {
            return ColumnType.BOOLEAN;
        }

        if (allMatch(presentValues, TypeInferrer::isIsoDate)) {
            return ColumnType.DATE;
        }

        return ColumnType.STRING;
    }

    private static boolean allMatch(
            List<String> values,
            Predicate<String> predicate) {

        for (String value : values) {
            if (!predicate.test(value)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isInteger(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isDecimal(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value)
                || "false".equalsIgnoreCase(value);
    }

    private static boolean isIsoDate(String value) {
        if (!ISO_DATE_PATTERN.matcher(value).matches()) {
            return false;
        }

        try {
            LocalDate.parse(
                    value,
                    DateTimeFormatter.ISO_LOCAL_DATE
            );
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}