package com.datakasauti.engine.clean;

/** Supported API cleaning operations, in execution order. */
public enum RuleType {
    TRIM_WHITESPACE,
    REMOVE_DUPLICATES,
    DROP_ROWS_WITH_MISSING,
    FILL_MISSING;

    public static RuleType from(String value) {
        try {
            return RuleType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidRuleException("Unknown cleaning rule type: " + value);
        }
    }
}
