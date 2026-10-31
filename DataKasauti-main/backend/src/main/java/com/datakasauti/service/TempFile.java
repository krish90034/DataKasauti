package com.datakasauti.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Owns one temporary upload. Closing it removes the stored file.
 */
public final class TempFile implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TempFile.class);

    private final Path path;

    TempFile(Path path) {
        this.path = path;
    }

    public Path path() {
        return path;
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            // Do not hide the API result; a startup stale-file sweep retries cleanup.
            log.warn("Could not delete temporary upload file {}", path.getFileName(), exception);
        }
    }
}
