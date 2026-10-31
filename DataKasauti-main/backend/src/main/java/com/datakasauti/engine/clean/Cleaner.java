 package com.datakasauti.engine.clean;

import com.datakasauti.dto.request.CleanRequest;
import com.datakasauti.dto.request.CleaningRuleDto;
import com.datakasauti.dto.response.CleanPreviewResponse;
import com.datakasauti.dto.response.RemovedRowDto;
import com.datakasauti.dto.response.RuleResultDto;
import com.datakasauti.dto.response.SampleChangeDto;
import com.datakasauti.engine.csv.ColumnType;
import com.datakasauti.engine.csv.CsvData;
import com.datakasauti.engine.csv.MissingValue;
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
import java.util.Set;

/** Applies the four supported cleaning rules in the contractually fixed order. */
public final class Cleaner {
    private static final int MAX_SAMPLES = 20;

    public CleanPreviewResult clean(CsvData csv, CleanRequest request) {
        validateRules(csv, request);
        List<WorkingRow> rows = new ArrayList<>();
        for (int i = 0; i < csv.rows().size(); i++) {
            rows.add(new WorkingRow(i + 1L, new ArrayList<>(csv.rows().get(i))));
        }

        List<RuleResultDto> ruleResults = new ArrayList<>();
        List<SampleChangeDto> sampleChanges = new ArrayList<>();
        List<RemovedRowDto> removedRows = new ArrayList<>();
        Map<String, CleaningRuleDto> singleRules = new LinkedHashMap<>();
        List<CleaningRuleDto> fillRules = new ArrayList<>();
        for (CleaningRuleDto rule : request.rules()) {
            if ("FILL_MISSING".equals(rule.type())) fillRules.add(rule);
            else singleRules.put(rule.type(), rule);
        }

        if (singleRules.containsKey("TRIM_WHITESPACE")) {
            ruleResults.add(applyTrim(rows, csv.headers(), sampleChanges));
        }
        if (singleRules.containsKey("REMOVE_DUPLICATES")) {
            ruleResults.add(applyRemoveDuplicates(rows, removedRows));
        }
        CleaningRuleDto dropRule = singleRules.get("DROP_ROWS_WITH_MISSING");
        if (dropRule != null) {
            ruleResults.add(applyDropMissing(rows, csv.headers(), dropRule, removedRows));
        }
        for (CleaningRuleDto rule : fillRules) {
            ruleResults.add(applyFillMissing(rows, csv.headers(), rule, sampleChanges));
        }

        List<List<String>> cleanedRows = rows.stream().map(row -> List.copyOf(row.values)).toList();
        CleanPreviewResponse preview = new CleanPreviewResponse(
                csv.headers(), csv.rowCount(), cleanedRows.size(), ruleResults,
                sampleChanges, removedRows, false);
        return new CleanPreviewResult(csv.headers(), cleanedRows, preview);
    }

    private void validateRules(CsvData csv, CleanRequest request) {
        if (request == null || request.rules() == null || request.rules().isEmpty()) {
            throw new InvalidRuleException("Provide at least one cleaning rule.");
        }
        Set<String> seenTypes = new HashSet<>();
        Set<String> filledColumns = new HashSet<>();
        for (CleaningRuleDto rule : request.rules()) {
            if (rule == null || rule.type() == null || rule.type().isBlank()) {
                throw new InvalidRuleException("Every rule must contain a type.");
            }
            CleaningRule parsedRule = CleaningRule.from(rule);
            switch (parsedRule.type()) {
                case TRIM_WHITESPACE, REMOVE_DUPLICATES -> {
                    if (!seenTypes.add(rule.type())) {
                        throw new InvalidRuleException("Rule " + rule.type() + " may only be applied once.");
                    }
                    if (rule.column() != null || rule.columns() != null
                            || rule.strategy() != null || rule.value() != null) {
                        throw new InvalidRuleException(rule.type() + " does not accept additional fields.");
                    }
                }
                case DROP_ROWS_WITH_MISSING -> {
                    if (!seenTypes.add(rule.type())) {
                        throw new InvalidRuleException("DROP_ROWS_WITH_MISSING may only be applied once.");
                    }
                    if (rule.columns() == null || rule.columns().isEmpty()
                            || rule.column() != null || rule.strategy() != null || rule.value() != null) {
                        throw new InvalidRuleException("DROP_ROWS_WITH_MISSING requires a non-empty columns array.");
                    }
                    Set<String> selected = new HashSet<>();
                    for (String column : rule.columns()) {
                        requireColumn(csv, column);
                        if (!selected.add(column)) {
                            throw new InvalidRuleException("Column " + column + " appears more than once in the rule.");
                        }
                    }
                }
                case FILL_MISSING -> validateFillRule(csv, rule, filledColumns);
                default -> throw new InvalidRuleException("Unknown cleaning rule type: " + rule.type());
            }
        }
    }

    private void validateFillRule(CsvData csv, CleaningRuleDto rule, Set<String> filledColumns) {
        requireColumn(csv, rule.column());
        if (rule.columns() != null || rule.strategy() == null || rule.strategy().isBlank()) {
            throw new InvalidRuleException("FILL_MISSING requires column and strategy.");
        }
        if (!filledColumns.add(rule.column())) {
            throw new InvalidRuleException("Only one FILL_MISSING rule is allowed per column: " + rule.column());
        }
        switch (FillStrategy.from(rule.strategy())) {
            case CONSTANT -> {
                if (rule.value() == null) throw new InvalidRuleException("CONSTANT strategy requires a value.");
            }
            case MEAN, MEDIAN -> {
                if (rule.value() != null) throw new InvalidRuleException(rule.strategy() + " does not accept a value field.");
                ColumnType type = csv.typeOf(rule.column());
                if (type == null || !type.isNumeric()) {
                    throw new InvalidRuleException(rule.strategy() + " can only be used with numeric columns: " + rule.column());
                }
            }
            case MODE -> {
                if (rule.value() != null) throw new InvalidRuleException("MODE does not accept a value field.");
            }
        }
    }

    private void requireColumn(CsvData csv, String column) {
        if (column == null || column.isBlank() || !csv.headers().contains(column)) {
            throw new InvalidRuleException("Unknown or missing column: " + column);
        }
    }

    private RuleResultDto applyTrim(List<WorkingRow> rows, List<String> headers,
                                    List<SampleChangeDto> samples) {
        long affectedRows = 0;
        long changedCells = 0;
        for (WorkingRow row : rows) {
            boolean rowChanged = false;
            for (int i = 0; i < row.values.size(); i++) {
                String before = row.values.get(i);
                String after = before.strip();
                if (!before.equals(after)) {
                    row.values.set(i, after);
                    rowChanged = true;
                    changedCells++;
                    addChangeSample(samples, row.rowNumber, headers.get(i), before, after);
                }
            }
            if (rowChanged) affectedRows++;
        }
        return new RuleResultDto("TRIM_WHITESPACE", null, affectedRows, changedCells);
    }

    private RuleResultDto applyRemoveDuplicates(List<WorkingRow> rows, List<RemovedRowDto> samples) {
        Set<List<String>> seen = new HashSet<>();
        long removed = 0;
        Iterator<WorkingRow> iterator = rows.iterator();
        while (iterator.hasNext()) {
            WorkingRow row = iterator.next();
            if (!seen.add(new ArrayList<>(row.values))) {
                addRemovedSample(samples, row, "REMOVE_DUPLICATES");
                iterator.remove();
                removed++;
            }
        }
        return new RuleResultDto("REMOVE_DUPLICATES", null, removed, 0);
    }

    private RuleResultDto applyDropMissing(List<WorkingRow> rows, List<String> headers,
                                           CleaningRuleDto rule, List<RemovedRowDto> samples) {
        Set<Integer> indexes = new LinkedHashSet<>();
        for (String column : rule.columns()) indexes.add(headers.indexOf(column));
        long removed = 0;
        Iterator<WorkingRow> iterator = rows.iterator();
        while (iterator.hasNext()) {
            WorkingRow row = iterator.next();
            boolean shouldRemove = indexes.stream().anyMatch(index -> MissingValue.isMissing(row.values.get(index)));
            if (shouldRemove) {
                addRemovedSample(samples, row, "DROP_ROWS_WITH_MISSING");
                iterator.remove();
                removed++;
            }
        }
        return new RuleResultDto("DROP_ROWS_WITH_MISSING", null, removed, 0);
    }

    private RuleResultDto applyFillMissing(List<WorkingRow> rows, List<String> headers,
                                           CleaningRuleDto rule, List<SampleChangeDto> samples) {
        int index = headers.indexOf(rule.column());
        String fillValue = switch (FillStrategy.from(rule.strategy())) {
            case CONSTANT -> rule.value();
            case MEAN -> calculateNumericFill(rows, index, false);
            case MEDIAN -> calculateNumericFill(rows, index, true);
            case MODE -> calculateMode(rows, index);
        };
        long changed = 0;
        for (WorkingRow row : rows) {
            String before = row.values.get(index);
            if (MissingValue.isMissing(before)) {
                row.values.set(index, fillValue);
                changed++;
                addChangeSample(samples, row.rowNumber, rule.column(), before, fillValue);
            }
        }
        return new RuleResultDto("FILL_MISSING", rule.column(), changed, changed);
    }

    private String calculateNumericFill(List<WorkingRow> rows, int index, boolean median) {
        List<BigDecimal> values = new ArrayList<>();
        for (WorkingRow row : rows) {
            String raw = row.values.get(index);
            if (!MissingValue.isMissing(raw)) {
                try {
                    values.add(new BigDecimal(raw.strip()));
                } catch (NumberFormatException exception) {
                    throw new InvalidRuleException("Numeric fill strategy found a non-numeric value.");
                }
            }
        }
        if (values.isEmpty()) throw new InvalidRuleException("Cannot calculate MEAN or MEDIAN because the column has no values.");
        values.sort(Comparator.naturalOrder());
        BigDecimal result;
        if (!median) {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal value : values) {
                sum = sum.add(value);
            }
            result = sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        } else {
            int middle = values.size() / 2;
            result = values.size() % 2 == 0
                    ? values.get(middle - 1).add(values.get(middle)).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                    : values.get(middle).setScale(2, RoundingMode.HALF_UP);
        }
        return result.stripTrailingZeros().toPlainString();
    }

    private String calculateMode(List<WorkingRow> rows, int index) {
        Map<String, Long> frequency = new LinkedHashMap<>();
        for (WorkingRow row : rows) {
            String value = row.values.get(index);
            if (!MissingValue.isMissing(value)) {
                Long currentCount = frequency.get(value);
                if (currentCount == null) {
                    frequency.put(value, 1L);
                } else {
                    frequency.put(value, currentCount + 1L);
                }
            }
        }
        if (frequency.isEmpty()) throw new InvalidRuleException("Cannot calculate MODE because the column has no values.");
        String mode = null;
        long highest = -1;
        for (Map.Entry<String, Long> entry : frequency.entrySet()) {
            if (entry.getValue() > highest) {
                mode = entry.getKey();
                highest = entry.getValue();
            }
        }
        return mode;
    }

    private void addChangeSample(List<SampleChangeDto> samples, long rowNumber,
                                 String column, String before, String after) {
        if (samples.size() < MAX_SAMPLES) samples.add(new SampleChangeDto(rowNumber, column, before, after));
    }

    private void addRemovedSample(List<RemovedRowDto> samples, WorkingRow row, String rule) {
        if (samples.size() < MAX_SAMPLES) samples.add(new RemovedRowDto(row.rowNumber, rule, List.copyOf(row.values)));
    }

    private static final class WorkingRow {
        private final long rowNumber;
        private final List<String> values;
        private WorkingRow(long rowNumber, List<String> values) {
            this.rowNumber = rowNumber;
            this.values = values;
        }
    }
}
