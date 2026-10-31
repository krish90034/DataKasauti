 package com.datakasauti.engine.profile;

import com.datakasauti.dto.response.ColumnProfileDto;
import com.datakasauti.dto.response.ColumnStatsDto;
import com.datakasauti.dto.response.WarningDto;
import com.datakasauti.engine.csv.ColumnType;
import com.datakasauti.engine.csv.CsvData;
import com.datakasauti.engine.csv.MissingValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure profile calculations. It has no knowledge of Spring, HTTP or uploaded files. */
public final class Profiler {
    private static final String ROW_SEPARATOR = "\u001F";
    private static final double HIGH_MISSING_PERCENT = 30.0;

    public ProfileResult profile(CsvData csv, String filename, long fileSizeBytes, int previewLimit) {
        long duplicateRows = countDuplicates(csv.rows());
        long totalMissing = 0;
        Map<String, Long> missingValues = new LinkedHashMap<>();
        List<ColumnProfileDto> columns = new ArrayList<>();
        List<WarningDto> warnings = new ArrayList<>();

        if (duplicateRows > 0) {
            warnings.add(new WarningDto("DUPLICATE_ROWS", "MEDIUM",
                    duplicateRows + " exact duplicate rows found"));
        }

        for (int columnIndex = 0; columnIndex < csv.columnCount(); columnIndex++) {
            String name = csv.headers().get(columnIndex);
            long missingCount = 0;
            ColumnType type = csv.typeOf(name);
            Set<String> uniqueValues = new HashSet<>();
            List<BigDecimal> numericValues = new ArrayList<>();

            for (List<String> row : csv.rows()) {
                String raw = row.get(columnIndex);
                if (MissingValue.isMissing(raw)) {
                    missingCount++;
                } else {
                    uniqueValues.add(raw);
                    if (type.isNumeric()) numericValues.add(new BigDecimal(raw.strip()));
                }
            }

            totalMissing += missingCount;
            if (missingCount > 0) missingValues.put(name, missingCount);

            double missingPercent = csv.rowCount() == 0 ? 0.0
                    : round(missingCount * 100.0 / csv.rowCount());
            ColumnStatsDto stats = type.isNumeric() ? numericStats(numericValues) : null;
            columns.add(new ColumnProfileDto(name, type.name(), missingCount,
                    missingPercent, uniqueValues.size(), stats));

            if (csv.rowCount() > 0 && missingPercent >= HIGH_MISSING_PERCENT) {
                warnings.add(new WarningDto("HIGH_MISSING_COLUMN", "MEDIUM",
                        "Column " + name + " has " + formatNumber(missingPercent) + "% missing values"));
            }
            if (csv.rowCount() > 0 && missingPercent == 100.0) {
                warnings.add(new WarningDto("EMPTY_COLUMN", "HIGH",
                        "Column " + name + " has no values"));
            }
        }

        List<List<String>> preview = new ArrayList<>();
        for (int i = 0; i < Math.min(csv.rows().size(), Math.max(0, previewLimit)); i++) {
            preview.add(new ArrayList<>(csv.rows().get(i)));
        }

        return new ProfileResult(filename, fileSizeBytes, csv.rowCount(), csv.columnCount(),
                duplicateRows, totalMissing, missingValues, columns, preview, warnings);
    }

    private long countDuplicates(List<List<String>> rows) {
        Set<String> seen = new HashSet<>();
        long duplicates = 0;
        for (List<String> row : rows) {
            if (!seen.add(String.join(ROW_SEPARATOR, row))) duplicates++;
        }
        return duplicates;
    }

    private ColumnStatsDto numericStats(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        List<BigDecimal> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        BigDecimal min = sorted.get(0);
        BigDecimal max = sorted.get(sorted.size() - 1);
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : sorted) {
            sum = sum.add(value);
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(sorted.size()), 2, RoundingMode.HALF_UP);
        int middle = sorted.size() / 2;
        BigDecimal median = sorted.size() % 2 == 0
                ? sorted.get(middle - 1).add(sorted.get(middle))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                : sorted.get(middle).setScale(2, RoundingMode.HALF_UP);
        return new ColumnStatsDto(min.doubleValue(), max.doubleValue(), mean.doubleValue(), median.doubleValue());
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

}
