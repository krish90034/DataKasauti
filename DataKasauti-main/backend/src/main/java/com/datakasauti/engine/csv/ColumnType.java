package com.datakasauti.engine.csv;

/** Inferred data types supported by DataKasauti. */
public enum ColumnType {
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,
    STRING;

    public boolean isNumeric() {
        return this == INTEGER || this == DECIMAL;
    }
}
