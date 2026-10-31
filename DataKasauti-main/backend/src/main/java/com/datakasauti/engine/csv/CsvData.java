package com.datakasauti.engine.csv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable parsed CSV shared by profile, compare and cleaning engines. */
public final class CsvData {
    private final List<String> headers;
    private final List<List<String>> rows;
    private final Map<String, ColumnType> columnTypes;

    public CsvData(List<String> headers, List<List<String>> rows) {
        this.headers = List.copyOf(headers);
        List<List<String>> copiedRows = new ArrayList<>(rows.size());
        for (List<String> row : rows) copiedRows.add(List.copyOf(row));
        this.rows = Collections.unmodifiableList(copiedRows);

        Map<String, ColumnType> inferred = new LinkedHashMap<>();
        for (int column = 0; column < this.headers.size(); column++) {
            List<String> values = new ArrayList<>(this.rows.size());
            for (List<String> row : this.rows) values.add(row.get(column));
            inferred.put(this.headers.get(column), TypeInferrer.infer(values));
        }
        this.columnTypes = Collections.unmodifiableMap(inferred);
    }

    public List<String> headers() { return headers; }
    public List<List<String>> rows() { return rows; }
    public int columnCount() { return headers.size(); }
    public long rowCount() { return rows.size(); }
    public ColumnType typeOf(String column) { return columnTypes.get(column); }
    public Map<String, ColumnType> columnTypes() { return columnTypes; }
}
