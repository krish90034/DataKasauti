package com.datakasauti.service;

import com.datakasauti.config.UploadProperties;
import com.datakasauti.exception.CsvApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Stores uploads under generated names and owns their temporary-file lifecycle.
 */
@Component
public class FileService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final Path tempDirectory;
    private final int staleFileMinutes;

    public FileService(UploadProperties uploadProperties) {
        String configuredDirectory = uploadProperties.getTempDir();
        this.tempDirectory = configuredDirectory == null || configuredDirectory.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "datakasauti")
                : Path.of(configuredDirectory);
        this.staleFileMinutes = uploadProperties.getStaleFileMinutes();
    }

    @Override
    public void run(ApplicationArguments arguments) {
        cleanupStaleFiles();
    }

    /**
     * Copies the uploaded bytes to a UUID-named file. The caller must use the
     * returned TempFile in try-with-resources so it is deleted after processing.
     */
    public TempFile store(MultipartFile upload) {
        Path storedFile = null;

        try {
            Files.createDirectories(tempDirectory);
            storedFile = Files.createTempFile(
                    tempDirectory,
                    "upload-" + UUID.randomUUID() + "-",
                    ".csv"
            );

            try (InputStream input = upload.getInputStream()) {
                Files.copy(input, storedFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return new TempFile(storedFile);
        } catch (IOException exception) {
            if (storedFile != null) {
                try {
                    Files.deleteIfExists(storedFile);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }

            log.error("Could not store uploaded CSV in the temporary directory", exception);
            throw new CsvApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR",
                    "The uploaded CSV could not be stored for processing.",
                    Map.of()
            );
        }
    }

    /**
     * Removes leftover files older than the configured retention period.
     * This runs once when the application starts and also can be invoked by tests.
     */
    public void cleanupStaleFiles() {
        if (staleFileMinutes < 1) {
            log.warn("Stale temporary-file cleanup is disabled because stale-file-minutes is below 1");
            return;
        }

        if (!Files.isDirectory(tempDirectory)) {
            return;
        }

        Instant cutoff = Instant.now().minus(staleFileMinutes, ChronoUnit.MINUTES);
        int deleted = 0;

        try (Stream<Path> files = Files.list(tempDirectory)) {
            for (Path file : files.toList()) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                FileTime modifiedTime = Files.getLastModifiedTime(file);
                if (modifiedTime.toInstant().isBefore(cutoff)) {
                    try {
                        if (Files.deleteIfExists(file)) {
                            deleted++;
                        }
                    } catch (IOException exception) {
                        log.warn("Could not remove stale temporary file {}", file.getFileName(), exception);
                    }
                }
            }
        } catch (IOException exception) {
            log.warn("Could not scan temporary directory for stale files", exception);
            return;
        }

        if (deleted > 0) {
            log.info("Removed {} stale DataKasauti temporary file(s)", deleted);
        }
    }
}
