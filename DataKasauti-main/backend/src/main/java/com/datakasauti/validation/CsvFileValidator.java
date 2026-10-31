 package com.datakasauti.validation;

import com.datakasauti.config.UploadProperties;
import com.datakasauti.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;

@Component
public class CsvFileValidator {
    private final UploadProperties uploadProperties;

    public CsvFileValidator(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public void validate(MultipartFile file) {
        if (file == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_MISSING",
                    "A CSV file is required."
            );
        }

        if (file.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_EMPTY",
                    "The uploaded file is empty."
            );
        }

        long maxBytes = (long) uploadProperties.getMaxUploadMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new ApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "FILE_TOO_LARGE",
                    "The uploaded file exceeds the " + uploadProperties.getMaxUploadMb() + " MB limit.",
                    Map.of("max_upload_mb", uploadProperties.getMaxUploadMb())
            );
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || !hasAllowedExtension(filename)) {
            throw new ApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "UNSUPPORTED_FILE_TYPE",
                    "Only CSV (.csv) files are supported.",
                    Map.of("allowed_extensions", uploadProperties.getAllowedExtensions())
            );
        }
    }

    private boolean hasAllowedExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return false;
        }

        String actualExtension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);

        String configuredExtensions = uploadProperties.getAllowedExtensions();
        if (configuredExtensions == null || configuredExtensions.isBlank()) {
            configuredExtensions = "csv";
        }

        String[] allowedExtensions = configuredExtensions.split(",");
        for (String configuredExtension : allowedExtensions) {
            if (configuredExtension == null) {
                continue;
            }

            String allowedExtension = configuredExtension.trim();
            if (allowedExtension.startsWith(".")) {
                allowedExtension = allowedExtension.substring(1);
            }

            if (actualExtension.equals(allowedExtension.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }
}
