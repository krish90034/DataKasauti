package com.datakasauti.engine.csv;

import java.util.LinkedHashMap;
import java.util.Map;

/** Engine-level CSV failure. HTTP status mapping belongs to the service/API layer. */
public final class CsvReadException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;

    public CsvReadException(String errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public CsvReadException(String errorCode, String message, Map<String, ?> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = new LinkedHashMap<>();
        if (details != null) this.details.putAll(details);
    }

    public String errorCode() {
        return errorCode;
    }

    public Map<String, Object> details() {
        return Map.copyOf(details);
    }
}
