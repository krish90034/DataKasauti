package com.datakasauti.engine.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Common UTF-8/BOM-aware CSV reader with the MVP row/column constraints. */
public final class CsvReader {
    private final int maxRows;
    private final int maxColumns;

    public CsvReader(int maxRows, int maxColumns) {
        this.maxRows = maxRows;
        this.maxColumns = maxColumns;
    }

    public CsvData read(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            return read(input);
        } catch (CsvReadException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new CsvReadException("CSV_PARSE_ERROR", "The uploaded CSV could not be parsed.");
        }
    }

    public CsvData read(InputStream input) {
        try (PushbackReader reader = new PushbackReader(
                new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)), 1)) {
            int first = reader.read();
            if (first != '\uFEFF' && first != -1) reader.unread(first);

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
                if (headers.size() > maxColumns) {
                    throw new CsvReadException("CSV_LIMIT_EXCEEDED",
                            "The CSV has more columns than allowed.",
                            Map.of("max_columns", maxColumns));
                }

                List<List<String>> rows = new ArrayList<>();
                long shortRows = 0;
                for (CSVRecord record : parser) {
                    if (rows.size() >= maxRows) {
                        throw new CsvReadException("CSV_LIMIT_EXCEEDED",
                                "The CSV has more data rows than allowed.",
                                Map.of("max_rows", maxRows));
                    }
                    if (record.size() > headers.size()) {
                        throw new CsvReadException("CSV_PARSE_ERROR",
                                "A CSV row contains more fields than the header.",
                                Map.of("record_number", record.getRecordNumber()));
                    }
                    List<String> row = new ArrayList<>(record.toList());
                    if (row.size() < headers.size()) shortRows++;
                    while (row.size() < headers.size()) row.add("");
                    rows.add(row);
                }

                if (!rows.isEmpty() && shortRows * 100L > rows.size() * 5L) {
                    throw new CsvReadException("CSV_PARSE_ERROR",
                            "More than 5% of data rows contain fewer fields than the header.",
                            Map.of("short_rows", shortRows, "total_rows", rows.size()));
                }
                return new CsvData(headers, rows);
            }
        } catch (CsvReadException exception) {
            throw exception;
        } catch (IOException | IllegalStateException exception) {
            throw new CsvReadException("CSV_PARSE_ERROR", "The uploaded CSV could not be parsed.");
        }
    }

    /** Reads and validates only the header. The supplied stream is closed. */
    public List<String> readHeader(InputStream input) {
        try (PushbackReader reader = new PushbackReader(
                new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)), 1)) {
            int first = reader.read();
            if (first != '\uFEFF' && first != -1) reader.unread(first);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true)
                    .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
                    .setAllowMissingColumnNames(true).get();
            try (CSVParser parser = format.parse(reader)) {
                List<String> headers = parser.getHeaderNames();
                validateHeaders(headers);
                if (headers.size() > maxColumns) {
                    throw new CsvReadException("CSV_LIMIT_EXCEEDED",
                            "The CSV has more columns than allowed.", Map.of("max_columns", maxColumns));
                }
                return List.copyOf(headers);
            }
        } catch (CsvReadException exception) {
            throw exception;
        } catch (IOException | IllegalStateException exception) {
            throw new CsvReadException("CSV_PARSE_ERROR", "The CSV header could not be parsed.");
        }
    }

    private void validateHeaders(List<String> headers) {
        if (headers == null || headers.isEmpty()
                || headers.stream().allMatch(value -> value == null || value.isBlank())) {
            throw new CsvReadException("FILE_EMPTY", "The CSV does not contain a header row with columns.");
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> blank = new ArrayList<>();
        List<String> duplicate = new ArrayList<>();
        for (String header : headers) {
            if (header == null || header.isBlank()) {
                blank.add(header == null ? "" : header);
            } else if (!seen.add(header)) {
                duplicate.add(header);
            }
        }
        if (!blank.isEmpty() || !duplicate.isEmpty()) {
            throw new CsvReadException("DUPLICATE_HEADERS",
                    "The CSV contains blank or duplicate column names.",
                    Map.of("blank_headers", blank, "duplicate_headers", duplicate));
        }
    }
}
