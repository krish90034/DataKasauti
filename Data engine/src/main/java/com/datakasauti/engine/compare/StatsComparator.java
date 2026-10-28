package com.datakasauti.engine.compare;

import com.datakasauti.engine.csv.ColumnType;
import com.datakasauti.engine.profile.ProfileResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StatsComparator {

    public List<CompareResult.CommonColumnStats> compare(
            ProfileResult baseline,
            ProfileResult current) {

        Objects.requireNonNull(baseline, "baseline must not be null");
        Objects.requireNonNull(current, "current must not be null");

        Map<String, ProfileResult.ColumnSummary> currentColumns =
                new LinkedHashMap<>();

        for (ProfileResult.ColumnSummary column : current.columns()) {
            currentColumns.put(column.name(), column);
        }

        List<CompareResult.CommonColumnStats> results =
                new ArrayList<>();

        for (ProfileResult.ColumnSummary baselineColumn
                : baseline.columns()) {

            ProfileResult.ColumnSummary currentColumn =
                    currentColumns.get(baselineColumn.name());

            if (currentColumn == null) {
                continue;
            }

            if (!isNumeric(baselineColumn.detectedType())
                    || !isNumeric(currentColumn.detectedType())) {
                continue;
            }

            ProfileResult.NumericStats baselineStats =
                    baselineColumn.stats();

            ProfileResult.NumericStats currentStats =
                    currentColumn.stats();

            if (baselineStats == null || currentStats == null) {
                continue;
            }

            long baselineCount = baseline.rowCount()
                    - baselineColumn.missingCount();

            long currentCount = current.rowCount()
                    - currentColumn.missingCount();

            CompareResult.SideStats baselineSide =
                    new CompareResult.SideStats(
                            baselineCount,
                            baselineColumn.missingCount(),
                            baselineStats.min(),
                            baselineStats.max(),
                            baselineStats.mean()
                    );

            CompareResult.SideStats currentSide =
                    new CompareResult.SideStats(
                            currentCount,
                            currentColumn.missingCount(),
                            currentStats.min(),
                            currentStats.max(),
                            currentStats.mean()
                    );

            double meanDelta = BigDecimal.valueOf(currentStats.mean())
                    .subtract(BigDecimal.valueOf(baselineStats.mean()))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            results.add(
                    new CompareResult.CommonColumnStats(
                            baselineColumn.name(),
                            baselineSide,
                            currentSide,
                            meanDelta
                    )
            );
        }

        return List.copyOf(results);
    }

    private boolean isNumeric(ColumnType type) {
        return type == ColumnType.INTEGER
                || type == ColumnType.DECIMAL;
    }
}