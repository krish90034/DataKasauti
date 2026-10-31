package com.datakasauti.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class CsvApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, Object> details;

    public CsvApiException(
            HttpStatus status,
            String errorCode,
            String message,
            Map<String, Object> details
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details == null ? Map.of() : details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}