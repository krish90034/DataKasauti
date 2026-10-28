package com.datakasauti.engine.clean;

import com.datakasauti.engine.csv.ColumnType;
import com.datakasauti.engine.csv.MissingValue;
import com.datakasauti.engine.csv.TypeInferrer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Cleaner {

    private static final int SAMPLE_LIMIT = 20;

    private record WorkingRow(
            int originalRowNumber,
            LinkedHashMap<String, String> values) {
    }

    public CleanPreviewResult clean(
            List<String> columns,
            List<Map<String, String>> rows,
            List<CleaningRule> rules) {

        Objects.requireNonNull(columns, "columns must not be null");
        Objects.requireNonNull(rows, "rows must not be null");
        Objects.requireNonNull(rules, "rules must not be null");

        List<String> headers = List.copyOf(columns);

        if (new LinkedHashSet<>(headers).size() != headers.size()) {
            throw invalidRule("Duplicate column names are not allowed");
        }

        List<WorkingRow> workingRows = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> inputRow = Objects.requireNonNull(
                    rows.get(i), "row must not be null");

            LinkedHashMap<String, String> values =
                    new LinkedHashMap<>();

            for (String header : headers) {
                values.put(header, inputRow.get(header));
            }

            workingRows.add(new WorkingRow(i + 1, values));
        }

        List<CleaningRule> orderedRules =
                validateAndOrder(headers, rules);

        List<CleanPreviewResult.RuleResult> ruleResults =
                new ArrayList<>();

        List<CleanPreviewResult.SampleChange> sampleChanges =
                new ArrayList<>();

        List<CleanPreviewResult.RemovedRowSample> removedRows =
                new ArrayList<>();

        long rowsBefore = workingRows.size();

        for (CleaningRule rule : orderedRules) {
            switch (rule.type()) {
                case TRIM_WHITESPACE -> {
                    long[] counts = trimWhitespace(
                            headers, workingRows, sampleChanges);

                    ruleResults.add(new CleanPreviewResult.RuleResult(
                            rule.type(), null, counts[0], counts[1]));
                }

                case REMOVE_DUPLICATES -> {
                    long removed = removeDuplicates(
                            headers, workingRows, removedRows);

                    ruleResults.add(new CleanPreviewResult.RuleResult(
                            rule.type(), null, removed, 0));
                }

                case DROP_ROWS_WITH_MISSING -> {
                    long removed = dropRowsWithMissing(
                            rule.columns(), headers,
                            workingRows, removedRows);

                    ruleResults.add(new CleanPreviewResult.RuleResult(
                            rule.type(), null, removed, 0));
                }

                case FILL_MISSING -> {
                    long[] counts = fillMissing(
                            rule, workingRows, sampleChanges);

                    ruleResults.add(new CleanPreviewResult.RuleResult(
                            rule.type(), rule.column(),
                            counts[0], counts[1]));
                }
            }
        }

        List<Map<String, String>> cleanedRows = new ArrayList<>();

        for (WorkingRow row : workingRows) {
            cleanedRows.add(new LinkedHashMap<>(row.values()));
        }

        return new CleanPreviewResult(
                headers,
                rowsBefore,
                workingRows.size(),
                ruleResults,
                sampleChanges,
                removedRows,
                false,
                cleanedRows
        );
    }

    private List<CleaningRule> validateAndOrder(
            List<String> headers,
            List<CleaningRule> rules) {

        Set<RuleType> singleRules = new HashSet<>();
        Set<String> fillColumns = new HashSet<>();
        List<CleaningRule> ordered = new ArrayList<>();

        for (CleaningRule rule : rules) {
            if (rule == null || rule.type() == null) {
                throw invalidRule("Rule type is required");
            }

            switch (rule.type()) {
                case TRIM_WHITESPACE, REMOVE_DUPLICATES -> {
                    if (!singleRules.add(rule.type())) {
                        throw invalidRule(
                                rule.type() + " may only appear once");
                    }
                }

                case DROP_ROWS_WITH_MISSING -> {
                    if (!singleRules.add(rule.type())) {
                        throw invalidRule(
                                "DROP_ROWS_WITH_MISSING may only appear once");
                    }

                    if (rule.columns().isEmpty()) {
                        throw invalidRule(
                                "Columns are required for DROP_ROWS_WITH_MISSING");
                    }

                    Set<String> selected = new HashSet<>();

                    for (String column : rule.columns()) {
                        validateColumn(headers, column);

                        if (!selected.add(column)) {
                            throw invalidRule(
                                    "Duplicate column in missing rule: " + column);
                        }
                    }
                }

                case FILL_MISSING -> {
                    validateColumn(headers, rule.column());

                    if (rule.strategy() == null) {
                        throw invalidRule("Fill strategy is required");
                    }

                    if (rule.strategy() == FillStrategy.CONSTANT
                            && rule.value() == null) {
                        throw invalidRule(
                                "CONSTANT strategy requires a value");
                    }

                    if (!fillColumns.add(rule.column())) {
                        throw invalidRule(
                                "Only one FILL_MISSING rule is allowed per column: "
                                        + rule.column());
                    }
                }
            }

            ordered.add(rule);
        }

        // Always apply cleaning rules in the required fixed order.
        ordered.sort(Comparator.comparingInt(
                rule -> rule.type().ordinal()));

        return ordered;
    }

    private void validateColumn(
            List<String> headers,
            String column) {

        if (column == null || !headers.contains(column)) {
            throw invalidRule("Unknown or missing column: " + column);
        }
    }

    private long[] trimWhitespace(
            List<String> headers,
            List<WorkingRow> rows,
            List<CleanPreviewResult.SampleChange> samples) {

        long rowsAffected = 0;
        long cellsChanged = 0;

        for (WorkingRow row : rows) {
            boolean rowChanged = false;

            for (String column : headers) {
                String before = row.values().get(column);

                if (before == null) {
                    continue;
                }

                String after = before.trim();

                if (!before.equals(after)) {
                    row.values().put(column, after);
                    rowChanged = true;
                    cellsChanged++;

                    addChangeSample(
                            samples, row.originalRowNumber(),
                            column, before, after);
                }
            }

            if (rowChanged) {
                rowsAffected++;
            }
        }

        return new long[]{rowsAffected, cellsChanged};
    }

    private long removeDuplicates(
            List<String> headers,
            List<WorkingRow> rows,
            List<CleanPreviewResult.RemovedRowSample> samples) {

        Set<List<String>> seen = new HashSet<>();
        long removed = 0;

        Iterator<WorkingRow> iterator = rows.iterator();

        while (iterator.hasNext()) {
            WorkingRow row = iterator.next();
            List<String> key = new ArrayList<>();

            for (String header : headers) {
                key.add(row.values().get(header));
            }

            if (!seen.add(key)) {
                addRemovedSample(
                        samples, headers, row, RuleType.REMOVE_DUPLICATES);

                iterator.remove();
                removed++;
            }
        }

        return removed;
    }

    private long dropRowsWithMissing(
            List<String> selectedColumns,
            List<String> headers,
            List<WorkingRow> rows,
            List<CleanPreviewResult.RemovedRowSample> samples) {

        long removed = 0;
        Iterator<WorkingRow> iterator = rows.iterator();

        while (iterator.hasNext()) {
            WorkingRow row = iterator.next();

            boolean shouldRemove = selectedColumns.stream()
                    .anyMatch(column ->
                            MissingValue.isMissing(row.values().get(column)));

            if (shouldRemove) {
                addRemovedSample(
                        samples, headers, row,
                        RuleType.DROP_ROWS_WITH_MISSING);

                iterator.remove();
                removed++;
            }
        }

        return removed;
    }

    private long[] fillMissing(
            CleaningRule rule,
            List<WorkingRow> rows,
            List<CleanPreviewResult.SampleChange> samples) {

        String column = rule.column();
        FillStrategy strategy = rule.strategy();

        String replacement;

        if (strategy == FillStrategy.CONSTANT) {
            replacement = rule.value();
        } else {
            List<String> values = new ArrayList<>();

            for (WorkingRow row : rows) {
                values.add(row.values().get(column));
            }

            if (strategy == FillStrategy.MEAN
                    || strategy == FillStrategy.MEDIAN) {

                ColumnType type = TypeInferrer.infer(values);

                if (type != ColumnType.INTEGER
                        && type != ColumnType.DECIMAL) {
                    throw invalidRule(
                            strategy + " requires a numeric column: " + column);
                }

                replacement = calculateNumericFill(values, strategy);
            } else {
                replacement = calculateMode(values);

                if (replacement == null) {
                    throw invalidRule(
                            "MODE needs a non-missing value in column: " + column);
                }
            }
        }

        long rowsAffected = 0;
        long cellsChanged = 0;

        for (WorkingRow row : rows) {
            String before = row.values().get(column);

            if (MissingValue.isMissing(before)) {
                row.values().put(column, replacement);
                rowsAffected++;
                cellsChanged++;

                addChangeSample(
                        samples, row.originalRowNumber(),
                        column, before, replacement);
            }
        }

        return new long[]{rowsAffected, cellsChanged};
    }

    private String calculateNumericFill(
            List<String> values,
            FillStrategy strategy) {

        List<BigDecimal> numbers = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;

        for (String value : values) {
            if (!MissingValue.isMissing(value)) {
                BigDecimal number = new BigDecimal(value.trim());
                numbers.add(number);
                sum = sum.add(number);
            }
        }

        if (numbers.isEmpty()) {
            throw invalidRule(
                    strategy + " cannot be calculated from empty values");
        }

        BigDecimal result;

        if (strategy == FillStrategy.MEAN) {
            result = sum.divide(
                    BigDecimal.valueOf(numbers.size()),
                    2,
                    RoundingMode.HALF_UP);
        } else {
            numbers.sort(BigDecimal::compareTo);

            int middle = numbers.size() / 2;

            if (numbers.size() % 2 == 1) {
                result = numbers.get(middle);
            } else {
                result = numbers.get(middle - 1)
                        .add(numbers.get(middle))
                        .divide(
                                BigDecimal.valueOf(2),
                                2,
                                RoundingMode.HALF_UP);
            }
        }

        return result.stripTrailingZeros().toPlainString();
    }

    private String calculateMode(List<String> values) {
        Map<String, Integer> frequencies = new LinkedHashMap<>();

        for (String value : values) {
            if (!MissingValue.isMissing(value)) {
                frequencies.merge(value, 1, Integer::sum);
            }
        }

        String mode = null;
        int highestFrequency = 0;

        for (Map.Entry<String, Integer> entry
                : frequencies.entrySet()) {

            if (entry.getValue() > highestFrequency) {
                highestFrequency = entry.getValue();
                mode = entry.getKey();
            }
        }

        // LinkedHashMap preserves first-seen order for ties.
        return mode;
    }

    private void addChangeSample(
            List<CleanPreviewResult.SampleChange> samples,
            int rowNumber,
            String column,
            String before,
            String after) {

        if (samples.size() < SAMPLE_LIMIT) {
            samples.add(new CleanPreviewResult.SampleChange(
                    rowNumber, column,
                    before == null ? "" : before,
                    after == null ? "" : after));
        }
    }

    private void addRemovedSample(
            List<CleanPreviewResult.RemovedRowSample> samples,
            List<String> headers,
            WorkingRow row,
            RuleType rule) {

        if (samples.size() >= SAMPLE_LIMIT) {
            return;
        }

        List<String> values = new ArrayList<>();

        for (String header : headers) {
            String value = row.values().get(header);
            values.add(value == null ? "" : value);
        }

        samples.add(new CleanPreviewResult.RemovedRowSample(
                row.originalRowNumber(),
                rule,
                values));
    }

    private IllegalArgumentException invalidRule(String message) {
        return new IllegalArgumentException("INVALID_RULE: " + message);
    }
}