package com.datakasauti.engine.clean;

/** Values supported by the FILL_MISSING operation. */
public enum FillStrategy {
    MEAN,
    MEDIAN,
    MODE,
    CONSTANT;

    public static FillStrategy from(String value) {
        try {
            return FillStrategy.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidRuleException("Unknown fill strategy: " + value);
        }
    }
}
