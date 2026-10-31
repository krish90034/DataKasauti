 package com.datakasauti.service;

import com.datakasauti.config.UploadProperties;
import com.datakasauti.dto.request.CleanRequest;
import com.datakasauti.dto.request.CleaningRuleDto;
import com.datakasauti.dto.response.CleanPreviewResponse;
import com.datakasauti.dto.response.RemovedRowDto;
import com.datakasauti.dto.response.RuleResultDto;
import com.datakasauti.dto.response.SampleChangeDto;
import com.datakasauti.exception.CsvApiException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
@SuppressWarnings("null")
@Service
public class DefaultCleanService implements CleanService {

    private static final int MAX_SAMPLES = 20;

    private final UploadProperties uploadProperties;
    private final FileService fileService;

    public DefaultCleanService(
            UploadProperties uploadProperties,
            FileService fileService
    ) {
        this.uploadProperties = uploadProperties;
        this.fileService = fileService;
    }

    @Override
    public CleanPreviewResponse preview(
            MultipartFile file,
            CleanRequest request
    ) {
        CleaningOutcome outcome = execute(file, request);
        return toResponse(outcome);
    }

    @Override
    public CleanExport export(
            MultipartFile file,
            CleanRequest request
    ) {
        CleaningOutcome outcome = execute(file, request);

        Path tempDir = getTempDirectory();
        Path outputFile = null;

        try {
            Files.createDirectories(tempDir);

            outputFile = Files.createTempFile(
                    tempDir,
                    "datakasauti-" + UUID.randomUUID() + "-",
                    ".csv"
            );

            writeCsv(outputFile, outcome);

            return new CleanExport(
                    outputFile,
                    makeDownloadName(file.getOriginalFilename())
            );

        } catch (IOException exception) {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                    // Preserve the original exception.
                }
            }

            throw new CsvApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR",
                    "Unable to create the cleaned CSV file.",
                    Map.of()
            );
        }
    }

    private CleaningOutcome execute(
            MultipartFile file,
            CleanRequest request
    ) {
        ParsedCsv parsed = readCsv(file);

        validateRules(request, parsed);

        List<WorkingRow> rows = new ArrayList<>();

        for (int i = 0; i < parsed.rows.size(); i++) {
            rows.add(new WorkingRow(
                    i + 1L,
                    new ArrayList<>(parsed.rows.get(i))
            ));
        }

        List<RuleResultDto> ruleResults = new ArrayList<>();
        List<SampleChangeDto> sampleChanges = new ArrayList<>();
        List<RemovedRowDto> removedRows = new ArrayList<>();

        Map<String, CleaningRuleDto> singleRules =
                new LinkedHashMap<>();

        List<CleaningRuleDto> fillRules = new ArrayList<>();

        for (CleaningRuleDto rule : request.rules()) {
            if ("FILL_MISSING".equals(rule.type())) {
                fillRules.add(rule);
            } else {
                singleRules.put(rule.type(), rule);
            }
        }

        // 1. Trim whitespace.
        if (singleRules.containsKey("TRIM_WHITESPACE")) {
            ruleResults.add(applyTrim(
                    rows,
                    parsed.headers,
                    sampleChanges
            ));
        }

        // 2. Remove duplicate rows, keeping the first.
        if (singleRules.containsKey("REMOVE_DUPLICATES")) {
            ruleResults.add(applyRemoveDuplicates(
                    rows,
                    removedRows
            ));
        }

        // 3. Drop rows that have missing values in selected columns.
        CleaningRuleDto dropRule =
                singleRules.get("DROP_ROWS_WITH_MISSING");

        if (dropRule != null) {
            ruleResults.add(applyDropMissing(
                    rows,
                    parsed.headers,
                    dropRule,
                    removedRows
            ));
        }

        // 4. Fill missing values in request order.
        for (CleaningRuleDto rule : fillRules) {
            ruleResults.add(applyFillMissing(
                    rows,
                    parsed.headers,
                    rule,
                    sampleChanges
            ));
        }

        return new CleaningOutcome(
                parsed.headers,
                parsed.rows.size(),
                rows,
                ruleResults,
                sampleChanges,
                removedRows
        );
    }

    private void validateRules(
            CleanRequest request,
            ParsedCsv csv
    ) {
        if (request == null
                || request.rules() == null
                || request.rules().isEmpty()) {
            throw invalidRule(
                    "Provide at least one cleaning rule."
            );
        }

        Set<String> seenTypes = new HashSet<>();
        Set<String> filledColumns = new HashSet<>();

        for (CleaningRuleDto rule : request.rules()) {

            if (rule == null
                    || rule.type() == null
                    || rule.type().isBlank()) {
                throw invalidRule(
                        "Every rule must contain a type."
                );
            }

            String type = rule.type();

            switch (type) {

                case "TRIM_WHITESPACE",
                     "REMOVE_DUPLICATES" -> {

                    if (!seenTypes.add(type)) {
                        throw invalidRule(
                                "Rule " + type
                                        + " may only be applied once."
                        );
                    }

                    if (rule.column() != null
                            || rule.columns() != null
                            || rule.strategy() != null
                            || rule.value() != null) {
                        throw invalidRule(
                                type + " does not accept additional fields."
                        );
                    }
                }

                case "DROP_ROWS_WITH_MISSING" -> {

                    if (!seenTypes.add(type)) {
                        throw invalidRule(
                                "DROP_ROWS_WITH_MISSING may only be applied once."
                        );
                    }

                    if (rule.columns() == null
                            || rule.columns().isEmpty()
                            || rule.column() != null
                            || rule.strategy() != null
                            || rule.value() != null) {
                        throw invalidRule(
                                "DROP_ROWS_WITH_MISSING requires a non-empty columns array."
                        );
                    }

                    Set<String> selected = new HashSet<>();

                    for (String column : rule.columns()) {
                        requireColumn(csv, column);

                        if (!selected.add(column)) {
                            throw invalidRule(
                                    "Column " + column
                                            + " appears more than once in the rule."
                            );
                        }
                    }
                }

                case "FILL_MISSING" -> {

                    requireColumn(csv, rule.column());

                    if (rule.columns() != null
                            || rule.strategy() == null
                            || rule.strategy().isBlank()) {
                        throw invalidRule(
                                "FILL_MISSING requires column and strategy."
                        );
                    }

                    if (!filledColumns.add(rule.column())) {
                        throw invalidRule(
                                "Only one FILL_MISSING rule is allowed per column: "
                                        + rule.column()
                        );
                    }

                    switch (rule.strategy()) {

                        case "CONSTANT" -> {
                            if (rule.value() == null) {
                                throw invalidRule(
                                        "CONSTANT strategy requires a value."
                                );
                            }
                        }

                        case "MEAN", "MEDIAN" -> {
                            if (rule.value() != null) {
                                throw invalidRule(
                                        rule.strategy()
                                                + " does not accept a value field."
                                );
                            }

                            // FIX: Do not redeclare the existing local
                            // variable named "type". Use "columnType".
                            String columnType = inferType(
                                    csv.rows,
                                    csv.headers.indexOf(rule.column())
                            );

                            if (!isNumeric(columnType)) {
                                throw invalidRule(
                                        rule.strategy()
                                                + " can only be used with numeric columns: "
                                                + rule.column()
                                );
                            }
                        }

                        case "MODE" -> {
                            if (rule.value() != null) {
                                throw invalidRule(
                                        "MODE does not accept a value field."
                                );
                            }
                        }

                        default -> throw invalidRule(
                                "Unknown fill strategy: "
                                        + rule.strategy()
                        );
                    }
                }

                default -> throw invalidRule(
                        "Unknown cleaning rule type: " + type
                );
            }
        }
    }

    private RuleResultDto applyTrim(
            List<WorkingRow> rows,
            List<String> headers,
            List<SampleChangeDto> samples
    ) {
        long affectedRows = 0;
        long changedCells = 0;

        for (WorkingRow row : rows) {
            boolean rowChanged = false;

            for (int i = 0; i < row.values.size(); i++) {
                String before = row.values.get(i);
                String after = before.strip();

                if (!before.equals(after)) {
                    row.values.set(i, after);
                    rowChanged = true;
                    changedCells++;

                    addChangeSample(
                            samples,
                            row.originalRowNumber,
                            headers.get(i),
                            before,
                            after
                    );
                }
            }

            if (rowChanged) {
                affectedRows++;
            }
        }

        return new RuleResultDto(
                "TRIM_WHITESPACE",
                null,
                affectedRows,
                changedCells
        );
    }

    private RuleResultDto applyRemoveDuplicates(
            List<WorkingRow> rows,
            List<RemovedRowDto> removedSamples
    ) {
        Set<List<String>> seenRows = new HashSet<>();
        long removedCount = 0;

        Iterator<WorkingRow> iterator = rows.iterator();

        while (iterator.hasNext()) {
            WorkingRow row = iterator.next();
            List<String> rowKey = new ArrayList<>(row.values);

            if (!seenRows.add(rowKey)) {
                addRemovedSample(
                        removedSamples,
                        row,
                        "REMOVE_DUPLICATES"
                );

                iterator.remove();
                removedCount++;
            }
        }

        return new RuleResultDto(
                "REMOVE_DUPLICATES",
                null,
                removedCount,
                0
        );
    }

    private RuleResultDto applyDropMissing(
            List<WorkingRow> rows,
            List<String> headers,
            CleaningRuleDto rule,
            List<RemovedRowDto> removedSamples
    ) {
        Set<Integer> columnIndexes = new LinkedHashSet<>();

        for (String column : rule.columns()) {
            columnIndexes.add(headers.indexOf(column));
        }

        long removedCount = 0;
        Iterator<WorkingRow> iterator = rows.iterator();

        while (iterator.hasNext()) {
            WorkingRow row = iterator.next();
            boolean shouldRemove = false;

            for (int index : columnIndexes) {
                if (isMissing(row.values.get(index))) {
                    shouldRemove = true;
                    break;
                }
            }

            if (shouldRemove) {
                addRemovedSample(
                        removedSamples,
                        row,
                        "DROP_ROWS_WITH_MISSING"
                );

                iterator.remove();
                removedCount++;
            }
        }

        return new RuleResultDto(
                "DROP_ROWS_WITH_MISSING",
                null,
                removedCount,
                0
        );
    }

    private RuleResultDto applyFillMissing(
            List<WorkingRow> rows,
            List<String> headers,
            CleaningRuleDto rule,
            List<SampleChangeDto> samples
    ) {
        int index = headers.indexOf(rule.column());

        String fillValue = switch (rule.strategy()) {
            case "CONSTANT" -> rule.value();
            case "MEAN" -> calculateNumericFill(rows, index, false);
            case "MEDIAN" -> calculateNumericFill(rows, index, true);
            case "MODE" -> calculateMode(rows, index);
            default -> throw invalidRule(
                    "Unknown fill strategy: " + rule.strategy()
            );
        };

        long cellsChanged = 0;

        for (WorkingRow row : rows) {
            String before = row.values.get(index);

            if (isMissing(before)) {
                row.values.set(index, fillValue);
                cellsChanged++;

                addChangeSample(
                        samples,
                        row.originalRowNumber,
                        rule.column(),
                        before,
                        fillValue
                );
            }
        }

        return new RuleResultDto(
                "FILL_MISSING",
                rule.column(),
                cellsChanged,
                cellsChanged
        );
    }

    private String calculateNumericFill(
            List<WorkingRow> rows,
            int columnIndex,
            boolean median
    ) {
        List<BigDecimal> values = new ArrayList<>();

        for (WorkingRow row : rows) {
            String value = row.values.get(columnIndex);

            if (!isMissing(value)) {
                try {
                    values.add(new BigDecimal(value.strip()));
                } catch (NumberFormatException exception) {
                    throw invalidRule(
                            "Numeric fill strategy found a non-numeric value."
                    );
                }
            }
        }

        if (values.isEmpty()) {
            throw invalidRule(
                    "Cannot calculate MEAN or MEDIAN because the column has no values."
            );
        }

        values.sort(BigDecimal::compareTo);

        BigDecimal result;

        if (!median) {
            BigDecimal sum = values.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result = sum.divide(
                    BigDecimal.valueOf(values.size()),
                    2,
                    RoundingMode.HALF_UP
            );
        } else {
            int middle = values.size() / 2;

            if (values.size() % 2 == 0) {
                result = values.get(middle - 1)
                        .add(values.get(middle))
                        .divide(
                                BigDecimal.valueOf(2),
                                2,
                                RoundingMode.HALF_UP
                        );
            } else {
                result = values.get(middle);
            }
        }

        return result.stripTrailingZeros().toPlainString();
    }

    private String calculateMode(
            List<WorkingRow> rows,
            int columnIndex
    ) {
        Map<String, Long> frequency = new LinkedHashMap<>();

        for (WorkingRow row : rows) {
            String value = row.values.get(columnIndex);

            if (!isMissing(value)) {
                frequency.merge(value, 1L, Long::sum);
            }
        }

        if (frequency.isEmpty()) {
            throw invalidRule(
                    "Cannot calculate MODE because the column has no values."
            );
        }

        String mode = null;
        long highestFrequency = -1;

        for (Map.Entry<String, Long> entry : frequency.entrySet()) {
            if (entry.getValue() > highestFrequency) {
                mode = entry.getKey();
                highestFrequency = entry.getValue();
            }
        }

        return mode;
    }

    private void addChangeSample(
            List<SampleChangeDto> samples,
            long rowNumber,
            String column,
            String before,
            String after
    ) {
        if (samples.size() < MAX_SAMPLES) {
            samples.add(new SampleChangeDto(
                    rowNumber,
                    column,
                    before,
                    after
            ));
        }
    }

    private void addRemovedSample(
            List<RemovedRowDto> samples,
            WorkingRow row,
            String rule
    ) {
        if (samples.size() < MAX_SAMPLES) {
            samples.add(new RemovedRowDto(
                    row.originalRowNumber,
                    rule,
                    new ArrayList<>(row.values)
            ));
        }
    }

    private CleanPreviewResponse toResponse(
            CleaningOutcome outcome
    ) {
        return new CleanPreviewResponse(
                outcome.headers,
                outcome.rowsBefore,
                outcome.rows.size(),
                outcome.ruleResults,
                outcome.sampleChanges,
                outcome.removedRowsSamples,
                false
        );
    }

    private ParsedCsv readCsv(MultipartFile file) {
        validateFile(file);

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
            int first = reader.read();

            if (first != '\uFEFF' && first != -1) {
                reader.unread(first);
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
                                "The CSV has more rows than allowed.",
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

                    List<String> row = new ArrayList<>(record.toList());

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
                            "More than 5% of rows have fewer fields than the header.",
                            Map.of(
                                    "short_rows", shortRows,
                                    "total_rows", rows.size()
                            )
                    );
                }

                return new ParsedCsv(headers, rows);
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
            String type = contentType.split(";")[0]
                    .trim()
                    .toLowerCase(Locale.ROOT);

            Set<String> supportedTypes = Set.of(
                    "text/csv",
                    "text/plain",
                    "application/vnd.ms-excel",
                    "application/octet-stream"
            );

            if (!supportedTypes.contains(type)) {
                throw new CsvApiException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "UNSUPPORTED_FILE_TYPE",
                        "The uploaded file content type is not supported.",
                        Map.of("content_type", type)
                );
            }
        }
    }

    private void validateHeaders(List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            throw new CsvApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "CSV_PARSE_ERROR",
                    "The CSV must contain a header row.",
                    Map.of()
            );
        }

        Set<String> seen = new LinkedHashSet<>();
        List<String> blankHeaders = new ArrayList<>();
        List<String> duplicateHeaders = new ArrayList<>();

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

    private void requireColumn(ParsedCsv csv, String column) {
        if (column == null
                || column.isBlank()
                || !csv.headers.contains(column)) {
            throw invalidRule(
                    "Unknown or missing column: " + column
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
            String value = raw.strip();

            boolean integer = false;

            try {
                Long.parseLong(value);
                integer = true;
            } catch (NumberFormatException ignored) {
                // Check decimal next.
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

    private boolean isNumeric(String type) {
        return "INTEGER".equals(type) || "DECIMAL".equals(type);
    }

    private boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value)
                || "false".equalsIgnoreCase(value);
    }

    private boolean isDate(String value) {
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

    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    private CsvApiException invalidRule(String message) {
        return new CsvApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_RULE",
                message,
                Map.of()
        );
    }

    private Path getTempDirectory() {
        String configured = uploadProperties.getTempDir();

        if (configured == null || configured.isBlank()) {
            return Path.of(
                    System.getProperty("java.io.tmpdir"),
                    "datakasauti"
            );
        }

        return Path.of(configured);
    }

    private String makeDownloadName(String originalFilename) {
        String name = originalFilename == null
                ? "dataset.csv"
                : originalFilename.replace("\\", "/");

        name = name.substring(name.lastIndexOf('/') + 1);

        if (name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            name = name.substring(0, name.length() - 4);
        }

        if (name.isBlank()) {
            name = "dataset";
        }

        return name + "_cleaned.csv";
    }

    private void writeCsv(
            Path outputFile,
            CleaningOutcome outcome
    ) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setRecordSeparator("\n")
                .get();

        try (
                BufferedWriter writer = Files.newBufferedWriter(
                        outputFile,
                        StandardCharsets.UTF_8
                );
                CSVPrinter printer = new CSVPrinter(writer, format)
        ) {
            List<String> safeHeaders = outcome.headers.stream()
                    .map(this::protectFormulaInjection)
                    .toList();
            printer.printRecord(safeHeaders);

            for (WorkingRow row : outcome.rows) {
                List<String> safeValues = new ArrayList<>();

                for (String value : row.values) {
                    safeValues.add(protectFormulaInjection(value));
                }

                printer.printRecord(safeValues);
            }
        }
    }

    private String protectFormulaInjection(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        char first = value.charAt(0);

        if (first != '=' && first != '+'
                && first != '-' && first != '@') {
            return value;
        }

        try {
            new BigDecimal(value.strip());
            return value;
        } catch (NumberFormatException exception) {
            return "'" + value;
        }
    }

    private static class ParsedCsv {
        private final List<String> headers;
        private final List<List<String>> rows;

        private ParsedCsv(
                List<String> headers,
                List<List<String>> rows
        ) {
            this.headers = headers;
            this.rows = rows;
        }
    }

    private static class WorkingRow {
        private final long originalRowNumber;
        private final List<String> values;

        private WorkingRow(
                long originalRowNumber,
                List<String> values
        ) {
            this.originalRowNumber = originalRowNumber;
            this.values = values;
        }
    }

    private static class CleaningOutcome {
        private final List<String> headers;
        private final long rowsBefore;
        private final List<WorkingRow> rows;
        private final List<RuleResultDto> ruleResults;
        private final List<SampleChangeDto> sampleChanges;
        private final List<RemovedRowDto> removedRowsSamples;

        private CleaningOutcome(
                List<String> headers,
                long rowsBefore,
                List<WorkingRow> rows,
                List<RuleResultDto> ruleResults,
                List<SampleChangeDto> sampleChanges,
                List<RemovedRowDto> removedRowsSamples
        ) {
            this.headers = headers;
            this.rowsBefore = rowsBefore;
            this.rows = rows;
            this.ruleResults = ruleResults;
            this.sampleChanges = sampleChanges;
            this.removedRowsSamples = removedRowsSamples;
        }
    }
}
