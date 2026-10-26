
package com.datakasauti.engine.csv;

public final class MissingValue {

    private MissingValue() {
    }

    public static boolean isMissing(String value) {
        return value == null || value.trim().isEmpty();
    }
}
