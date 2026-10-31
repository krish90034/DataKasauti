package com.datakasauti.engine.csv;

/** Shared definition of a missing CSV cell for every engine operation. */
public final class MissingValue {
    private MissingValue() {
    }

    public static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}
