 package com.datakasauti.service;

import com.datakasauti.config.UploadProperties;
import com.datakasauti.dto.response.ColumnProfileDto;
import com.datakasauti.dto.response.ColumnStatsDto;
import com.datakasauti.dto.response.ProfileResponse;
import com.datakasauti.dto.response.WarningDto;
import com.datakasauti.exception.CsvApiException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
@SuppressWarnings("null")
@Service
public class DefaultProfileService implements ProfileService {

    private static final String ROW_SEPARATOR = "\u001F";

    private final UploadProperties uploadProperties;
    private final FileService fileService;

    public DefaultProfileService(
            UploadProperties uploadProperties,
            FileService fileService
    ) {
        this.uploadProperties = uploadProperties;
        this.fileService = fileService;
    }

    @Override
    public ProfileResponse profile(MultipartFile file) {

        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "upload.csv";
        }

        try (TempFile storedUpload = fileService.store(file)) {
            return analyzeFile(storedUpload.path(), originalFilename, file.getSize());
        } catch (CsvApiException exception) {
            throw exception;
        } catch (IOException | IllegalStateException exception) {
            throw new CsvApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "CSV_PARSE_ERROR",
                    "The uploaded CSV could not be parsed.",
                    Map.of()
            );
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null) {
            throw new CsvApiException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_MISSING",
                    "Please upload a CSV file.",
                    Map.of()
            );
        }

        if (file.isEmpty()) {
            throw new CsvApiException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_EMPTY",
                    "The uploaded file is empty.",
                    Map.of()
            );
        }

        long maxBytes =
                uploadProperties.getMaxUploadMb() * 1024L * 1024L;

        if (file.getSize() > maxBytes) {
            throw new CsvApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "FILE_TOO_LARGE",
                    "The uploaded file exceeds the maximum allowed size.",
                    Map.of("max_upload_mb", uploadProperties.getMaxUploadMb())
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null
                || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {

            throw new CsvApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "UNSUPPORTED_FILE_TYPE",
                    "Only CSV files are supported.",
                    Map.of("allowed_extension", "csv")
            );
        }

        String contentType = file.getContentType();

        if (contentType != null && !contentType.isBlank()) {

            String mimeType = contentType
                    .split(";")[0]
                    .trim()
                    .toLowerCase(Locale.ROOT);

            Set<String> allowedTypes = Set.of(
                    "text/csv",
                    "text/plain",
                    "application/vnd.ms-excel",
                    "application/octet-stream"
            );

            if (!allowedTypes.contains(mimeType)) {
                throw new CsvApiException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "UNSUPPORTED_FILE_TYPE",
                        "The uploaded file content type is not supported.",
                        Map.of("content_type", mimeType)
                );
            }
        }
    }

    private ProfileResponse analyzeFile(
            Path filePath,
            String originalFilename,
            long fileSizeBytes
    ) throws IOException {
        try (
                InputStream input = Files.newInputStream(filePath);
                PushbackReader reader = new PushbackReader(
                        new BufferedReader(
                                new InputStreamReader(
                                        input,
                                        StandardCharsets.UTF_8
                                )
                        ),
                        1
                )
        ) {
            // Remove UTF-8 BOM if the file contains one.
            int firstCharacter = reader.read();

            if (firstCharacter != '\uFEFF' && firstCharacter != -1) {
                reader.unread(firstCharacter);
            }

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(false)
                    .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
                    .setAllowMissingColumnNames(true)
                    .get();
            try (CSVParser parser = format.parse(reader)) {

                List<String> headers = parser.getHeaderNames();

                validateHeaders(headers);

                int columnCount = headers.size();

                if (columnCount > uploadProperties.getMaxColumns()) {
                    throw new CsvApiException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "CSV_LIMIT_EXCEEDED",
                            "The CSV has more columns than allowed.",
                            Map.of(
                                    "max_columns",
                                    uploadProperties.getMaxColumns()
                            )
                    );
                }

                List<ColumnAccumulator> accumulators =
                        new ArrayList<>();

                for (String header : headers) {
                    accumulators.add(new ColumnAccumulator(header));
                }

                Set<String> seenRows = new HashSet<>();

                List<List<String>> previewRows =
                        new ArrayList<>();

                long rowCount = 0;
                long duplicateRows = 0;
                long shortRows = 0;

                for (CSVRecord record : parser) {

                    rowCount++;

                    if (rowCount > uploadProperties.getMaxRows()) {
                        throw new CsvApiException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "CSV_LIMIT_EXCEEDED",
                                "The CSV has more data rows than allowed.",
                                Map.of(
                                        "max_rows",
                                        uploadProperties.getMaxRows()
                                )
                        );
                    }

                    if (record.size() > columnCount) {
                        throw new CsvApiException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "CSV_PARSE_ERROR",
                                "A CSV row contains more fields than the header.",
                                Map.of("record_number", record.getRecordNumber())
                        );
                    }

                    List<String> row = new ArrayList<>(record.toList());

                    if (row.size() < columnCount) {
                        shortRows++;
                    }

                    while (row.size() < columnCount) {
                        row.add("");
                    }

                    // Three identical rows count as two duplicates.
                    String rowKey = String.join(ROW_SEPARATOR, row);

                    if (!seenRows.add(rowKey)) {
                        duplicateRows++;
                    }

                    for (int i = 0; i < columnCount; i++) {
                        accumulators.get(i).add(row.get(i));
                    }

                    if (previewRows.size()
                            < uploadProperties.getPreviewRows()) {
                        previewRows.add(new ArrayList<>(row));
                    }
                }

                // More than 5% short rows means this CSV is malformed.
                if (rowCount > 0 && shortRows * 100 > rowCount * 5) {
                    throw new CsvApiException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "CSV_PARSE_ERROR",
                            "More than 5% of the CSV data rows have fewer fields than the header.",
                            Map.of(
                                    "short_rows", shortRows,
                                    "total_rows", rowCount
                            )
                    );
                }

                return buildResponse(
                        originalFilename,
                        fileSizeBytes,
                        rowCount,
                        duplicateRows,
                        headers,
                        accumulators,
                        previewRows
                );
            }
        }
    }

    private void validateHeaders(List<String> headers) {

        if (headers == null || headers.isEmpty()) {
            throw new CsvApiException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_EMPTY",
                    "The CSV does not contain a header row with columns.",
                    Map.of()
            );
        }

        List<String> blankHeaders = new ArrayList<>();
        List<String> duplicateHeaders = new ArrayList<>();
        Set<String> seenHeaders = new LinkedHashSet<>();

        for (String header : headers) {

            if (header == null || header.isBlank()) {
                blankHeaders.add(header == null ? "" : header);
                continue;
            }

            if (!seenHeaders.add(header)) {
                duplicateHeaders.add(header);
            }
        }

        if (!blankHeaders.isEmpty() || !duplicateHeaders.isEmpty()) {
            throw new CsvApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "DUPLICATE_HEADERS",
                    "The CSV contains blank or duplicate column names.",
                    Map.of(
                            "blank_headers", blankHeaders,
                            "duplicate_headers", duplicateHeaders
                    )
            );
        }
    }

    private ProfileResponse buildResponse(
            String filename,
            long fileSizeBytes,
            long rowCount,
            long duplicateRows,
            List<String> headers,
            List<ColumnAccumulator> accumulators,
            List<List<String>> previewRows
    ) {

        List<ColumnProfileDto> columns = new ArrayList<>();

        Map<String, Long> missingValues = new LinkedHashMap<>();

        List<WarningDto> warnings = new ArrayList<>();

        long totalMissingValues = 0;

        if (duplicateRows > 0) {
            warnings.add(
                    new WarningDto(
                            "DUPLICATE_ROWS",
                            "MEDIUM",
                            duplicateRows + " exact duplicate rows found"
                    )
            );
        }

        for (ColumnAccumulator accumulator : accumulators) {

            long missingCount = accumulator.missingCount;

            totalMissingValues += missingCount;

            if (missingCount > 0) {
                missingValues.put(accumulator.name, missingCount);
            }

            double missingPercent = rowCount == 0
                    ? 0.0
                    : BigDecimal.valueOf(missingCount)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    BigDecimal.valueOf(rowCount),
                                    2,
                                    RoundingMode.HALF_UP
                            )
                            .doubleValue();

            String detectedType = accumulator.detectedType();

            ColumnStatsDto stats = null;

            if ("INTEGER".equals(detectedType)
                    || "DECIMAL".equals(detectedType)) {
                stats = accumulator.statistics();
            }

            columns.add(
                    new ColumnProfileDto(
                            accumulator.name,
                            detectedType,
                            missingCount,
                            missingPercent,
                            accumulator.uniqueValues.size(),
                            stats
                    )
            );

            if (rowCount > 0 && missingPercent >= 30.0) {
                warnings.add(
                        new WarningDto(
                                "HIGH_MISSING_COLUMN",
                                "MEDIUM",
                                "Column " + accumulator.name
                                        + " has "
                                        + formatNumber(missingPercent)
                                        + "% missing values"
                        )
                );
            }

            if (rowCount > 0 && missingPercent == 100.0) {
                warnings.add(
                        new WarningDto(
                                "EMPTY_COLUMN",
                                "HIGH",
                                "Column " + accumulator.name
                                        + " has no values"
                        )
                );
            }
        }

        return new ProfileResponse(
                filename,
                fileSizeBytes,
                rowCount,
                headers.size(),
                duplicateRows,
                totalMissingValues,
                missingValues,
                columns,
                previewRows,
                warnings
        );
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value)
                || "false".equalsIgnoreCase(value);
    }

    private static boolean isDate(String value) {
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }

        try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static class ColumnAccumulator {

        private final String name;

        private long missingCount;
        private long nonMissingCount;

        private boolean allInteger = true;
        private boolean allDecimal = true;
        private boolean allBoolean = true;
        private boolean allDate = true;

        private final Set<String> uniqueValues = new HashSet<>();
        private final List<BigDecimal> numericValues = new ArrayList<>();

        private ColumnAccumulator(String name) {
            this.name = name;
        }

        private void add(String rawValue) {

            if (isMissing(rawValue)) {
                missingCount++;
                return;
            }

            nonMissingCount++;
            uniqueValues.add(rawValue);

            String value = rawValue.trim();

            boolean integer = false;

            try {
                Long.parseLong(value);
                integer = true;
            } catch (NumberFormatException ignored) {
                // It may still be a DECIMAL.
            }

            if (!integer) {
                allInteger = false;
            }

            try {
                numericValues.add(new BigDecimal(value));
            } catch (NumberFormatException exception) {
                allDecimal = false;
            }

            if (!isBoolean(value)) {
                allBoolean = false;
            }

            if (!isDate(value)) {
                allDate = false;
            }
        }

        private String detectedType() {

            if (nonMissingCount == 0) {
                return "STRING";
            }

            if (allInteger) {
                return "INTEGER";
            }

            if (allDecimal) {
                return "DECIMAL";
            }

            if (allBoolean) {
                return "BOOLEAN";
            }

            if (allDate) {
                return "DATE";
            }

            return "STRING";
        }

        private ColumnStatsDto statistics() {

            if (numericValues.isEmpty()) {
                return null;
            }

            List<BigDecimal> sorted =
                    new ArrayList<>(numericValues);

            Collections.sort(sorted);

            BigDecimal min = sorted.get(0);
            BigDecimal max = sorted.get(sorted.size() - 1);

            BigDecimal sum = sorted.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal mean = sum.divide(
                    BigDecimal.valueOf(sorted.size()),
                    2,
                    RoundingMode.HALF_UP
            );

            int middle = sorted.size() / 2;

            BigDecimal median;

            if (sorted.size() % 2 == 0) {
                median = sorted.get(middle - 1)
                        .add(sorted.get(middle))
                        .divide(
                                BigDecimal.valueOf(2),
                                2,
                                RoundingMode.HALF_UP
                        );
            } else {
                median = sorted.get(middle)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            return new ColumnStatsDto(
                    min.doubleValue(),
                    max.doubleValue(),
                    mean.doubleValue(),
                    median.doubleValue()
            );
        }
    }
}
