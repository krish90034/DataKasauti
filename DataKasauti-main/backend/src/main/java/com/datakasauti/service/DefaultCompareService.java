 package com.datakasauti.service;

import com.datakasauti.config.UploadProperties;
import com.datakasauti.dto.response.CommonColumnDto;
import com.datakasauti.dto.response.CompareResponse;
import com.datakasauti.dto.response.FileSummaryDto;
import com.datakasauti.dto.response.RowChangesDto;
import com.datakasauti.dto.response.SchemaChangeDto;
import com.datakasauti.dto.response.SideStatsDto;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class DefaultCompareService implements CompareService {

    private final UploadProperties uploadProperties;
    private final FileService fileService;

    public DefaultCompareService(
            UploadProperties uploadProperties,
            FileService fileService
    ) {
        this.uploadProperties = uploadProperties;
        this.fileService = fileService;
    }

    @Override
    public CompareResponse compare(
            MultipartFile baselineFile,
            MultipartFile currentFile,
            String keyColumn
    ) {
        ParsedCsv baseline = readCsv(baselineFile);
        ParsedCsv current = readCsv(currentFile);

        List<String> addedColumns = new ArrayList<>();
        List<String> removedColumns = new ArrayList<>();
        List<SchemaChangeDto> typeChanges = new ArrayList<>();
        List<CommonColumnDto> commonColumns = new ArrayList<>();
        List<WarningDto> warnings = new ArrayList<>();

        // Columns that exist only in the current file.
        for (String column : current.headers) {
            if (!baseline.headers.contains(column)) {
                addedColumns.add(column);

                warnings.add(new WarningDto(
                        "COLUMN_ADDED",
                        "LOW",
                        "Column " + column
                                + " exists in current only"
                ));
            }
        }

        // Columns that exist only in the baseline file.
        for (String column : baseline.headers) {
            if (!current.headers.contains(column)) {
                removedColumns.add(column);

                warnings.add(new WarningDto(
                        "COLUMN_REMOVED",
                        "HIGH",
                        "Column " + column
                                + " exists in baseline only"
                ));
            }
        }

        // Compare matching columns in baseline order.
        for (String column : baseline.headers) {

            if (!current.headers.contains(column)) {
                continue;
            }

            String baselineType = baseline.typeOf(column);
            String currentType = current.typeOf(column);

            if (!baselineType.equals(currentType)) {
                typeChanges.add(new SchemaChangeDto(
                        column,
                        baselineType,
                        currentType
                ));

                warnings.add(new WarningDto(
                        "TYPE_CHANGED",
                        "HIGH",
                        "Column " + column
                                + " changed type from "
                                + baselineType + " to "
                                + currentType
                ));
            }

            // Numeric statistics are returned only when both sides
            // have numeric types.
            if (isNumeric(baselineType)
                    && isNumeric(currentType)) {

                SideStatsDto baselineStats =
                        calculateStats(baseline, column);

                SideStatsDto currentStats =
                        calculateStats(current, column);

                double meanDelta = round2(
                        BigDecimal.valueOf(currentStats.mean())
                                .subtract(
                                        BigDecimal.valueOf(
                                                baselineStats.mean()
                                        )
                                )
                );

                commonColumns.add(new CommonColumnDto(
                        column,
                        baselineStats,
                        currentStats,
                        meanDelta
                ));
            }
        }

        long rowCountDelta =
                (long) current.rows.size() - baseline.rows.size();

        if (rowCountDelta != 0) {
            String signedDelta = rowCountDelta > 0
                    ? "+" + rowCountDelta
                    : String.valueOf(rowCountDelta);

            warnings.add(new WarningDto(
                    "ROW_COUNT_CHANGED",
                    "LOW",
                    "Row count changed from "
                            + baseline.rows.size()
                            + " to " + current.rows.size()
                            + " (" + signedDelta + ")"
            ));
        }

        RowChangesDto rowChanges = null;

        if (keyColumn != null && !keyColumn.isBlank()) {
            rowChanges = compareRows(
                    baseline,
                    current,
                    keyColumn.trim(),
                    warnings
            );
        }

        return new CompareResponse(
                new FileSummaryDto(
                        baseline.filename,
                        baseline.rows.size(),
                        baseline.headers.size()
                ),
                new FileSummaryDto(
                        current.filename,
                        current.rows.size(),
                        current.headers.size()
                ),
                rowCountDelta,
                addedColumns,
                removedColumns,
                typeChanges,
                commonColumns,
                rowChanges,
                warnings
        );
    }

    private ParsedCsv readCsv(MultipartFile file) {

        validateFile(file);

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            filename = "upload.csv";
        }

        try (
                TempFile storedUpload = fileService.store(file);
                InputStream input = Files.newInputStream(storedUpload.path());
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
            // Remove the UTF-8 BOM, if present.
            int first = reader.read();

            if (first != '\uFEFF' && first != -1) {
                reader.unread(first);
            }

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(false)
                    .setDuplicateHeaderMode(
                            DuplicateHeaderMode.ALLOW_ALL
                    )
                    .setAllowMissingColumnNames(true)
                    .get();

            try (CSVParser parser = format.parse(reader)) {

                List<String> headers = parser.getHeaderNames();

                validateHeaders(headers);

                if (headers.size() > uploadProperties.getMaxColumns()) {
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

                List<List<String>> rows = new ArrayList<>();
                long shortRows = 0;

                for (CSVRecord record : parser) {

                    if (rows.size() >= uploadProperties.getMaxRows()) {
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

                    if (record.size() > headers.size()) {
                        throw new CsvApiException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "CSV_PARSE_ERROR",
                                "A CSV row contains more fields than the header.",
                                Map.of(
                                        "record_number",
                                        record.getRecordNumber()
                                )
                        );
                    }

                    List<String> row =
                            new ArrayList<>(record.toList());

                    if (row.size() < headers.size()) {
                        shortRows++;
                    }

                    while (row.size() < headers.size()) {
                        row.add("");
                    }

                    rows.add(row);
                }

                if (!rows.isEmpty()
                        && shortRows * 100L > rows.size() * 5L) {
                    throw new CsvApiException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "CSV_PARSE_ERROR",
                            "More than 5% of data rows contain fewer fields than the header.",
                            Map.of(
                                    "short_rows", shortRows,
                                    "total_rows", rows.size()
                            )
                    );
                }

                Map<String, String> types = new HashMap<>();

                for (int index = 0; index < headers.size(); index++) {
                    types.put(
                            headers.get(index),
                            inferType(rows, index)
                    );
                }

                return new ParsedCsv(
                        filename,
                        headers,
                        rows,
                        types
                );
            }

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
                    Map.of(
                            "max_upload_mb",
                            uploadProperties.getMaxUploadMb()
                    )
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

            String mimeType = contentType.split(";")[0]
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
        Set<String> seen = new LinkedHashSet<>();

        for (String header : headers) {

            if (header == null || header.isBlank()) {
                blankHeaders.add(header == null ? "" : header);
                continue;
            }

            if (!seen.add(header)) {
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

    private String inferType(
            List<List<String>> rows,
            int columnIndex
    ) {
        boolean allInteger = true;
        boolean allDecimal = true;
        boolean allBoolean = true;
        boolean allDate = true;
        long nonMissing = 0;

        for (List<String> row : rows) {

            String raw = row.get(columnIndex);

            if (isMissing(raw)) {
                continue;
            }

            nonMissing++;

            String value = raw.trim();

            boolean integer = false;

            try {
                Long.parseLong(value);
                integer = true;
            } catch (NumberFormatException ignored) {
                // Try decimal below.
            }

            if (!integer) {
                allInteger = false;
            }

            try {
                new BigDecimal(value);
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

        if (nonMissing == 0) {
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

    private SideStatsDto calculateStats(
            ParsedCsv csv,
            String column
    ) {
        int index = csv.headers.indexOf(column);

        long missing = 0;
        List<BigDecimal> values = new ArrayList<>();

        for (List<String> row : csv.rows) {

            String raw = row.get(index);

            if (isMissing(raw)) {
                missing++;
            } else {
                values.add(new BigDecimal(raw.trim()));
            }
        }

        if (values.isEmpty()) {
            return new SideStatsDto(0, missing, 0, 0, 0);
        }

        BigDecimal min = values.get(0);
        BigDecimal max = values.get(0);
        BigDecimal sum = BigDecimal.ZERO;

        for (BigDecimal value : values) {

            if (value.compareTo(min) < 0) {
                min = value;
            }

            if (value.compareTo(max) > 0) {
                max = value;
            }

            sum = sum.add(value);
        }

        BigDecimal mean = sum.divide(
                BigDecimal.valueOf(values.size()),
                2,
                RoundingMode.HALF_UP
        );

        return new SideStatsDto(
                values.size(),
                missing,
                min.doubleValue(),
                max.doubleValue(),
                mean.doubleValue()
        );
    }

    private RowChangesDto compareRows(
            ParsedCsv baseline,
            ParsedCsv current,
            String keyColumn,
            List<WarningDto> warnings
    ) {
        boolean baselineHasKey =
                baseline.headers.contains(keyColumn);

        boolean currentHasKey =
                current.headers.contains(keyColumn);

        if (!baselineHasKey || !currentHasKey) {

            List<String> missingFrom = new ArrayList<>();

            if (!baselineHasKey) {
                missingFrom.add("baseline");
            }

            if (!currentHasKey) {
                missingFrom.add("current");
            }

            warnings.add(new WarningDto(
                    "KEY_COLUMN_NOT_FOUND",
                    "MEDIUM",
                    "Key column " + keyColumn
                            + " not found in "
                            + String.join(" and ", missingFrom)
            ));

            return null;
        }

        boolean baselineInvalid =
                hasInvalidKeys(baseline, keyColumn);

        boolean currentInvalid =
                hasInvalidKeys(current, keyColumn);

        if (baselineInvalid || currentInvalid) {

            List<String> invalidFiles = new ArrayList<>();

            if (baselineInvalid) {
                invalidFiles.add("baseline");
            }

            if (currentInvalid) {
                invalidFiles.add("current");
            }

            warnings.add(new WarningDto(
                    "KEY_NOT_UNIQUE",
                    "MEDIUM",
                    "Key column " + keyColumn
                            + " has duplicate or blank values in "
                            + String.join(" and ", invalidFiles)
            ));

            return null;
        }

        Map<String, List<String>> baselineRows =
                rowsByKey(baseline, keyColumn);

        Map<String, List<String>> currentRows =
                rowsByKey(current, keyColumn);

        long added = currentRows.keySet().stream()
                .filter(key -> !baselineRows.containsKey(key))
                .count();

        long removed = baselineRows.keySet().stream()
                .filter(key -> !currentRows.containsKey(key))
                .count();

        long unchanged = 0;
        long changed = 0;

        // Compare values for keys present in both datasets.
        // Only columns common to both files are considered.
        for (String key : baselineRows.keySet()) {

            if (!currentRows.containsKey(key)) {
                continue;
            }

            List<String> baselineRow = baselineRows.get(key);
            List<String> currentRow = currentRows.get(key);

            boolean rowChanged = false;

            for (String column : baseline.headers) {

                if (column.equals(keyColumn)
                        || !current.headers.contains(column)) {
                    continue;
                }

                int baselineIndex =
                        baseline.headers.indexOf(column);

                int currentIndex =
                        current.headers.indexOf(column);

                if (!Objects.equals(
                        baselineRow.get(baselineIndex),
                        currentRow.get(currentIndex)
                )) {
                    rowChanged = true;
                    break;
                }
            }

            if (rowChanged) {
                changed++;
            } else {
                unchanged++;
            }
        }

        return new RowChangesDto(
                keyColumn,
                added,
                removed,
                unchanged,
                changed
        );
    }

    private boolean hasInvalidKeys(
            ParsedCsv csv,
            String keyColumn
    ) {
        int index = csv.headers.indexOf(keyColumn);
        Set<String> seen = new HashSet<>();

        for (List<String> row : csv.rows) {

            String key = row.get(index);

            if (isMissing(key) || !seen.add(key)) {
                return true;
            }
        }

        return false;
    }

    private Map<String, List<String>> rowsByKey(
            ParsedCsv csv,
            String keyColumn
    ) {
        int index = csv.headers.indexOf(keyColumn);
        Map<String, List<String>> result = new HashMap<>();

        for (List<String> row : csv.rows) {
            result.put(row.get(index), row);
        }

        return result;
    }

    private boolean isNumeric(String type) {
        return "INTEGER".equals(type) || "DECIMAL".equals(type);
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

    private double round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static class ParsedCsv {

        private final String filename;
        private final List<String> headers;
        private final List<List<String>> rows;
        private final Map<String, String> types;

        private ParsedCsv(
                String filename,
                List<String> headers,
                List<List<String>> rows,
                Map<String, String> types
        ) {
            this.filename = filename;
            this.headers = headers;
            this.rows = rows;
            this.types = types;
        }

        private String typeOf(String column) {
            return types.get(column);
        }
    }
}