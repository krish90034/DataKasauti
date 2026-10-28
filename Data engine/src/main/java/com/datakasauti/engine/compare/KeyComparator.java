package com.datakasauti.engine.compare;

import com.datakasauti.engine.csv.MissingValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class KeyComparator {

    public record Result(
            CompareResult.RowChanges rowChanges,
            List<String> warnings
    ) {
        public Result {
            warnings = List.copyOf(warnings);
        }
    }

    private record IndexResult(
            Map<String, Map<String, String>> rowsByKey,
            String warning
    ) {
    }

    public Result compare(
            List<String> baselineHeaders,
            List<Map<String, String>> baselineRows,
            List<String> currentHeaders,
            List<Map<String, String>> currentRows,
            String keyColumn) {

        Objects.requireNonNull(baselineHeaders);
        Objects.requireNonNull(baselineRows);
        Objects.requireNonNull(currentHeaders);
        Objects.requireNonNull(currentRows);

        if (keyColumn == null || keyColumn.isBlank()) {
            return new Result(null, List.of());
        }

        if (!baselineHeaders.contains(keyColumn)
                || !currentHeaders.contains(keyColumn)) {

            List<String> missingFrom = new ArrayList<>();

            if (!baselineHeaders.contains(keyColumn)) {
                missingFrom.add("baseline");
            }

            if (!currentHeaders.contains(keyColumn)) {
                missingFrom.add("current");
            }

            return new Result(
                    null,
                    List.of(
                            "KEY_COLUMN_NOT_FOUND: Key column '"
                                    + keyColumn
                                    + "' not found in "
                                    + String.join(" and ", missingFrom)
                    )
            );
        }

        IndexResult baselineIndex = buildIndex(
                baselineRows, keyColumn, "baseline"
        );

        if (baselineIndex.warning() != null) {
            return new Result(
                    null,
                    List.of(baselineIndex.warning())
            );
        }

        IndexResult currentIndex = buildIndex(
                currentRows, keyColumn, "current"
        );

        if (currentIndex.warning() != null) {
            return new Result(
                    null,
                    List.of(currentIndex.warning())
            );
        }

        Map<String, Map<String, String>> baseline =
                baselineIndex.rowsByKey();

        Map<String, Map<String, String>> current =
                currentIndex.rowsByKey();

        long added = 0;
        long removed = 0;
        long unchanged = 0;
        long changed = 0;

        for (String key : current.keySet()) {
            if (!baseline.containsKey(key)) {
                added++;
            }
        }

        for (Map.Entry<String, Map<String, String>> entry
                : baseline.entrySet()) {

            String key = entry.getKey();

            if (!current.containsKey(key)) {
                removed++;
                continue;
            }

            if (haveSameValues(
                    entry.getValue(),
                    current.get(key),
                    baselineHeaders,
                    currentHeaders)) {
                unchanged++;
            } else {
                changed++;
            }
        }

        CompareResult.RowChanges rowChanges =
                new CompareResult.RowChanges(
                        keyColumn,
                        added,
                        removed,
                        unchanged,
                        changed
                );

        return new Result(rowChanges, List.of());
    }

    private IndexResult buildIndex(
            List<Map<String, String>> rows,
            String keyColumn,
            String datasetName) {

        Map<String, Map<String, String>> index =
                new LinkedHashMap<>();

        for (Map<String, String> row : rows) {
            String key = row.get(keyColumn);

            if (MissingValue.isMissing(key)) {
                return new IndexResult(
                        null,
                        "KEY_NOT_UNIQUE: Key column '"
                                + keyColumn
                                + "' contains a missing value in "
                                + datasetName
                );
            }

            if (index.containsKey(key)) {
                return new IndexResult(
                        null,
                        "KEY_NOT_UNIQUE: Key column '"
                                + keyColumn
                                + "' contains a duplicate value in "
                                + datasetName
                );
            }

            index.put(key, row);
        }

        return new IndexResult(index, null);
    }

    private boolean haveSameValues(
            Map<String, String> baselineRow,
            Map<String, String> currentRow,
            List<String> baselineHeaders,
            List<String> currentHeaders) {

        Set<String> commonHeaders = new LinkedHashSet<>(
                baselineHeaders
        );

        commonHeaders.retainAll(currentHeaders);

        for (String header : commonHeaders) {
            if (!Objects.equals(
                    baselineRow.get(header),
                    currentRow.get(header))) {
                return false;
            }
        }

        return true;
    }
}