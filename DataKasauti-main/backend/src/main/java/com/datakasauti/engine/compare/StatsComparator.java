package com.datakasauti.engine.compare;

import com.datakasauti.dto.response.CommonColumnDto;
import com.datakasauti.dto.response.SideStatsDto;
import com.datakasauti.engine.csv.CsvData;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Numeric column statistics for compare. Statistics use the same rounding policy as profile. */
public final class StatsComparator {
    public List<CommonColumnDto> compare(CsvData baseline, CsvData current) {
        List<CommonColumnDto> result = new ArrayList<>();
        for (String column : baseline.headers()) {
            if (!current.headers().contains(column)) continue;
            if (baseline.typeOf(column) == null || current.typeOf(column) == null
                    || !baseline.typeOf(column).isNumeric() || !current.typeOf(column).isNumeric()) {
                continue;
            }
            SideStatsDto left = stats(baseline, column);
            SideStatsDto right = stats(current, column);
            double delta = round2(BigDecimal.valueOf(right.mean()).subtract(BigDecimal.valueOf(left.mean())));
            result.add(new CommonColumnDto(column, left, right, delta));
        }
        return List.copyOf(result);
    }

    public SideStatsDto stats(CsvData csv, String column) {
        int index = csv.headers().indexOf(column);
        long missing = 0;
        List<BigDecimal> values = new ArrayList<>();
        for (List<String> row : csv.rows()) {
            String raw = row.get(index);
            if (raw == null || raw.isBlank()) {
                missing++;
            } else {
                values.add(new BigDecimal(raw.strip()));
            }
        }
        if (values.isEmpty()) return new SideStatsDto(0, missing, 0, 0, 0);
        BigDecimal min = values.get(0);
        BigDecimal max = values.get(0);
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value.compareTo(min) < 0) min = value;
            if (value.compareTo(max) > 0) max = value;
            sum = sum.add(value);
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        return new SideStatsDto(values.size(), missing, min.doubleValue(), max.doubleValue(), mean.doubleValue());
    }

    private double round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
