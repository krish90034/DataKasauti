package com.datakasauti.engine.compare;

import com.datakasauti.dto.response.FileSummaryDto;
import com.datakasauti.dto.response.WarningDto;
import com.datakasauti.engine.csv.CsvData;
import java.util.ArrayList;
import java.util.List;

/** Coordinates schema, numeric-statistics and key comparators without depending on Spring. */
public final class CompareEngine {
    private final SchemaComparator schemaComparator = new SchemaComparator();
    private final StatsComparator statsComparator = new StatsComparator();
    private final KeyComparator keyComparator = new KeyComparator();

    public CompareResult compare(CsvData baseline, String baselineFilename,
                                 CsvData current, String currentFilename, String keyColumn) {
        SchemaComparison schema = schemaComparator.compare(baseline, current);
        KeyComparison key = keyComparator.compare(baseline, current, keyColumn);
        long rowCountDelta = current.rowCount() - baseline.rowCount();
        List<WarningDto> warnings = new ArrayList<>(schema.warnings());
        if (rowCountDelta != 0) {
            String signed = rowCountDelta > 0 ? "+" + rowCountDelta : String.valueOf(rowCountDelta);
            warnings.add(new WarningDto("ROW_COUNT_CHANGED", "LOW",
                    "Row count changed from " + baseline.rowCount() + " to " + current.rowCount()
                            + " (" + signed + ")"));
        }
        warnings.addAll(key.warnings());
        return new CompareResult(
                new FileSummaryDto(baselineFilename, baseline.rowCount(), baseline.columnCount()),
                new FileSummaryDto(currentFilename, current.rowCount(), current.columnCount()),
                rowCountDelta, schema.addedColumns(), schema.removedColumns(), schema.typeChanges(),
                statsComparator.compare(baseline, current), key.rowChanges(), warnings);
    }
}
