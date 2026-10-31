package com.datakasauti.engine.clean;

/** Pure-engine validation failure; the service layer maps it to HTTP 400. */
public final class InvalidRuleException extends RuntimeException {
    public InvalidRuleException(String message) {
        super(message);
    }
}
