 package com.datakasauti.engine.compare;

import com.datakasauti.dto.response.RowChangesDto;
import com.datakasauti.dto.response.WarningDto;
import com.datakasauti.engine.csv.CsvData;
import com.datakasauti.engine.csv.MissingValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Optional row-level comparison using one unique, non-missing key column. */
public final class KeyComparator {
    public KeyComparison compare(CsvData baseline, CsvData current, String keyColumn) {
        if (keyColumn == null || keyColumn.isBlank()) return new KeyComparison(null, List.of());
        List<WarningDto> warnings = new ArrayList<>();
        boolean hasBaseline = baseline.headers().contains(keyColumn);
        boolean hasCurrent = current.headers().contains(keyColumn);
        if (!hasBaseline || !hasCurrent) {
            List<String> missingFrom = new ArrayList<>();
            if (!hasBaseline) missingFrom.add("baseline");
            if (!hasCurrent) missingFrom.add("current");
            warnings.add(new WarningDto("KEY_COLUMN_NOT_FOUND", "MEDIUM",
                    "Key column " + keyColumn + " not found in " + String.join(" and ", missingFrom)));
            return new KeyComparison(null, List.copyOf(warnings));
        }

        if (hasInvalidKeys(baseline, keyColumn) || hasInvalidKeys(current, keyColumn)) {
            List<String> invalidFiles = new ArrayList<>();
            if (hasInvalidKeys(baseline, keyColumn)) invalidFiles.add("baseline");
            if (hasInvalidKeys(current, keyColumn)) invalidFiles.add("current");
            warnings.add(new WarningDto("KEY_NOT_UNIQUE", "MEDIUM",
                    "Key column " + keyColumn + " has duplicate or blank values in " + String.join(" and ", invalidFiles)));
            return new KeyComparison(null, List.copyOf(warnings));
        }

        Map<String, List<String>> left = rowsByKey(baseline, keyColumn);
        Map<String, List<String>> right = rowsByKey(current, keyColumn);
        long added = right.keySet().stream().filter(key -> !left.containsKey(key)).count();
        long removed = left.keySet().stream().filter(key -> !right.containsKey(key)).count();
        long unchanged = 0;
        long changed = 0;

        int leftKeyIndex = baseline.headers().indexOf(keyColumn);
        for (Map.Entry<String, List<String>> entry : left.entrySet()) {
            List<String> rightRow = right.get(entry.getKey());
            if (rightRow == null) continue;
            List<String> leftRow = entry.getValue();
            boolean differs = false;
            for (int i = 0; i < baseline.headers().size(); i++) {
                String column = baseline.headers().get(i);
                if (i == leftKeyIndex || !current.headers().contains(column)) continue;
                int rightIndex = current.headers().indexOf(column);
                if (!Objects.equals(leftRow.get(i), rightRow.get(rightIndex))) {
                    differs = true;
                    break;
                }
            }
            if (differs) changed++; else unchanged++;
        }
        return new KeyComparison(new RowChangesDto(keyColumn, added, removed, unchanged, changed), List.of());
    }

    private boolean hasInvalidKeys(CsvData csv, String keyColumn) {
        int index = csv.headers().indexOf(keyColumn);
        Set<String> seen = new HashSet<>();
        for (List<String> row : csv.rows()) {
            String key = row.get(index);
            if (MissingValue.isMissing(key) || !seen.add(key)) return true;
        }
        return false;
    }

    private Map<String, List<String>> rowsByKey(CsvData csv, String keyColumn) {
        int index = csv.headers().indexOf(keyColumn);
        Map<String, List<String>> rows = new HashMap<>();
        for (List<String> row : csv.rows()) rows.put(row.get(index), row);
        return rows;
    }
}
