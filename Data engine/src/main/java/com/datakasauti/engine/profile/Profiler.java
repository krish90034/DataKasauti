package com.datakasauti.engine.profile;

import com.datakasauti.engine.csv.ColumnType;
import com.datakasauti.engine.csv.CsvReader;
import com.datakasauti.engine.csv.MissingValue;
import com.datakasauti.engine.csv.TypeInferrer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Profiler {

    public static final int PREVIEW_LIMIT = 20;

    private final CsvReader csvReader = new CsvReader();

    public ProfileResult profile(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");

        List<Map<String, String>> rows = csvReader.read(path);

        List<String> headers;

        if (rows.isEmpty()) {
            try (var input = Files.newInputStream(path)) {
                headers = CsvReader.readHeader(input);
            }
        } else {
            headers = new ArrayList<>(rows.get(0).keySet());
        }

        Map<String, List<String>> columnValues =
                new LinkedHashMap<>();

        Map<String, Long> missingValues =
                new LinkedHashMap<>();

        Map<String, Set<String>> uniqueValues =
                new LinkedHashMap<>();

        for (String header : headers) {
            columnValues.put(header, new ArrayList<>());
            uniqueValues.put(header, new HashSet<>());
            missingValues.put(header, 0L);
        }

        Set<List<String>> seenRows = new HashSet<>();
        long duplicateRows = 0;

        List<List<String>> previewRows = new ArrayList<>();

        for (Map<String, String> row : rows) {
            List<String> rowValues = new ArrayList<>();

            for (String header : headers) {
                String value = row.get(header);

                rowValues.add(value);
                columnValues.get(header).add(value);

                if (MissingValue.isMissing(value)) {
                    missingValues.put(
                            header,
                            missingValues.get(header) + 1
                    );
                } else {
                    uniqueValues.get(header).add(value);
                }
            }

            List<String> rowKey = Collections.unmodifiableList(
                    new ArrayList<>(rowValues)
            );

            if (!seenRows.add(rowKey)) {
                duplicateRows++;
            }

            if (previewRows.size() < PREVIEW_LIMIT) {
                List<String> preview = new ArrayList<>();

                for (String value : rowValues) {
                    preview.add(value == null ? "" : value);
                }

                previewRows.add(preview);
            }
        }

        long rowCount = rows.size();
        long totalMissing = 0;

        Map<String, Long> reportedMissing =
                new LinkedHashMap<>();

        List<ProfileResult.ColumnSummary> summaries =
                new ArrayList<>();

        for (String header : headers) {
            List<String> values = columnValues.get(header);

            long missingCount = missingValues.get(header);
            totalMissing += missingCount;

            if (missingCount > 0) {
                reportedMissing.put(header, missingCount);
            }

            ColumnType type = TypeInferrer.infer(values);

            double missingPercent = rowCount == 0
                    ? 0.0
                    : BigDecimal.valueOf(missingCount)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    BigDecimal.valueOf(rowCount),
                                    2,
                                    RoundingMode.HALF_UP
                            )
                            .doubleValue();

            ProfileResult.NumericStats stats = null;

            if (type == ColumnType.INTEGER
                    || type == ColumnType.DECIMAL) {
                stats = calculateStats(values);
            }

            summaries.add(
                    new ProfileResult.ColumnSummary(
                            header,
                            type,
                            missingCount,
                            missingPercent,
                            uniqueValues.get(header).size(),
                            stats
                    )
            );
        }

        return new ProfileResult(
                rowCount,
                headers.size(),
                duplicateRows,
                totalMissing,
                reportedMissing,
                summaries,
                previewRows
        );
    }

    private ProfileResult.NumericStats calculateStats(
            List<String> values) {

        List<BigDecimal> numbers = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;

        for (String value : values) {
            if (MissingValue.isMissing(value)) {
                continue;
            }

            BigDecimal number = new BigDecimal(value.trim());
            numbers.add(number);
            sum = sum.add(number);
        }

        if (numbers.isEmpty()) {
            return null;
        }

        numbers.sort(BigDecimal::compareTo);

        BigDecimal min = numbers.get(0);
        BigDecimal max = numbers.get(numbers.size() - 1);

        BigDecimal mean = sum.divide(
                BigDecimal.valueOf(numbers.size()),
                2,
                RoundingMode.HALF_UP
        );

        int middle = numbers.size() / 2;
        BigDecimal median;

        if (numbers.size() % 2 == 1) {
            median = numbers.get(middle);
        } else {
            median = numbers.get(middle - 1)
                    .add(numbers.get(middle))
                    .divide(
                            BigDecimal.valueOf(2),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        return new ProfileResult.NumericStats(
                min.doubleValue(),
                max.doubleValue(),
                mean.doubleValue(),
                median.doubleValue()
        );
    }
}