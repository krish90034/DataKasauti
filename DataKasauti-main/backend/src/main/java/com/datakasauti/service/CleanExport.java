package com.datakasauti.service;

import java.nio.file.Path;

public record CleanExport(
        Path tempFile,
        String downloadName
) {
}