 package com.datakasauti.exception;

import com.datakasauti.dto.response.ApiErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException exception
    ) {
        return response(
                exception.getStatus(),
                exception.getErrorCode(),
                exception.getMessage(),
                exception.getDetails()
        );
    }

    @ExceptionHandler(CsvApiException.class)
    public ResponseEntity<ApiErrorResponse> handleCsvApiException(
            CsvApiException exception
    ) {
        return response(
                exception.getStatus(),
                exception.getErrorCode(),
                exception.getMessage(),
                exception.getDetails()
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(
            MissingServletRequestPartException exception
    ) {
        String partName = exception.getRequestPartName();

        return response(
                HttpStatus.BAD_REQUEST,
                "FILE_MISSING",
                "Required multipart part is missing: " + partName,
                Map.of("part", partName == null ? "unknown" : partName)
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleFileTooLarge(
            MaxUploadSizeExceededException exception
    ) {
        return response(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                "The uploaded file exceeds the maximum allowed size.",
                Map.of()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception
    ) {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_FILE_TYPE",
                "The request content type is not supported.",
                Map.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_RULE",
                "Required request parameter is missing: "
                        + exception.getParameterName(),
                Map.of("parameter", exception.getParameterName())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed.");

        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_RULE",
                message,
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception
    ) {
        log.error(
                "Unexpected server error while processing request",
                exception
        );

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected server error occurred.",
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String errorCode,
            String message,
            Map<String, Object> details
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                errorCode,
                message,
                toDetailList(details)
        );

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Internal exceptions keep structured map details. The public API contract
     * exposes details as a JSON array of readable strings.
     */
    private List<String> toDetailList(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return List.of();
        }

        List<String> formatted = new ArrayList<>();
        details.forEach((key, value) ->
                formatted.add(key + ": " + String.valueOf(value))
        );
        return List.copyOf(formatted);
    }
}
