package com.datakasauti.engine.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CsvReader {

    public static final int MAX_DATA_ROWS = 200_000;
    public static final int MAX_COLUMNS = 200;
    private static final int MAX_SHORT_ROW_PERCENT = 5;

    private static final CSVFormat CSV_FORMAT =
            CSVFormat.DEFAULT.builder()
                    .setIgnoreEmptyLines(false)
                    .get();

    /**
     * CSV file ko UTF-8 format mein read karta hai.
     * Input stream read hone ke baad close ho jayegi.
     */
    public List<Map<String, String>> read(Path path)
            throws IOException {

        Objects.requireNonNull(path, "path must not be null");

        return read(Files.newInputStream(path));
    }

    /**
     * Input stream se CSV rows read karta hai.
     * Har row mein column header aur uski value ka mapping hota hai.
     */
    public List<Map<String, String>> read(InputStream inputStream)
            throws IOException {

        Objects.requireNonNull(
                inputStream,
                "inputStream must not be null"
        );

        try (BufferedInputStream input =
                     new BufferedInputStream(inputStream)) {

            stripUtf8Bom(input);

            try (Reader reader = new InputStreamReader(
                        input, StandardCharsets.UTF_8);
                 CSVParser parser = CSVParser.parse(
                        reader, CSV_FORMAT)) {

                Iterator<CSVRecord> records = parser.iterator();

                try {
                    if (!records.hasNext()) {
                        throw new CsvParseException(
                                "CSV_PARSE_ERROR: CSV file has no header"
                        );
                    }

                    List<String> headers =
                            validateHeaders(records.next());

                    List<Map<String, String>> rows =
                            new ArrayList<>();

                    int dataRows = 0;
                    int shortRows = 0;

                    while (records.hasNext()) {
                        CSVRecord record = records.next();

                        if (dataRows >= MAX_DATA_ROWS) {
                            throw new CsvLimitException(
                                    "CSV_LIMIT_EXCEEDED: Maximum "
                                            + MAX_DATA_ROWS
                                            + " data rows allowed"
                            );
                        }

                        int rowNumber = dataRows + 1;
                        dataRows++;

                        if (record.size() > headers.size()) {
                            throw new CsvParseException(
                                    "CSV_PARSE_ERROR: Row "
                                            + rowNumber
                                            + " contains more fields "
                                            + "than the header"
                            );
                        }

                        if (record.size() < headers.size()) {
                            shortRows++;
                        }

                        Map<String, String> row =
                                new LinkedHashMap<>();

                        for (int i = 0; i < headers.size(); i++) {
                            String value = i < record.size()
                                    ? record.get(i)
                                    : "";

                            row.put(headers.get(i), value);
                        }

                        rows.add(row);
                    }

                    if (dataRows > 0
                            && shortRows * 100
                            > dataRows * MAX_SHORT_ROW_PERCENT) {

                        throw new CsvParseException(
                                "CSV_PARSE_ERROR: More than 5% "
                                        + "of data rows have missing "
                                        + "trailing fields"
                        );
                    }

                    return rows;

                } catch (UncheckedIOException exception) {
                    throw new CsvParseException(
                            "CSV_PARSE_ERROR: Unable to parse CSV",
                            exception.getCause()
                    );
                }
            }
        }
    }

    /**
     * Sirf CSV header read karta hai.
     * Validation ke baad input stream close ho jayegi.
     */
    public static List<String> readHeader(InputStream inputStream)
            throws IOException {

        Objects.requireNonNull(
                inputStream,
                "inputStream must not be null"
        );

        try (BufferedInputStream input =
                     new BufferedInputStream(inputStream)) {

            stripUtf8Bom(input);

            try (Reader reader = new InputStreamReader(
                        input, StandardCharsets.UTF_8);
                 CSVParser parser = CSVParser.parse(
                        reader, CSV_FORMAT)) {

                Iterator<CSVRecord> records = parser.iterator();

                try {
                    if (!records.hasNext()) {
                        throw new CsvParseException(
                                "CSV_PARSE_ERROR: CSV file has no header"
                        );
                    }

                    return validateHeaders(records.next());

                } catch (UncheckedIOException exception) {
                    throw new CsvParseException(
                            "CSV_PARSE_ERROR: Unable to read CSV header",
                            exception.getCause()
                    );
                }
            }
        }
    }

    private static List<String> validateHeaders(CSVRecord record)
            throws IOException {

        if (record.size() == 0) {
            throw new CsvParseException(
                    "CSV_PARSE_ERROR: Header is empty"
            );
        }

        if (record.size() > MAX_COLUMNS) {
            throw new CsvLimitException(
                    "CSV_LIMIT_EXCEEDED: Maximum "
                            + MAX_COLUMNS + " columns allowed"
            );
        }

        List<String> headers = new ArrayList<>();
        Set<String> seenHeaders = new HashSet<>();

        for (int i = 0; i < record.size(); i++) {
            String header = record.get(i);

            // Safety fallback in case a BOM remains in the first header.
            if (i == 0 && header.startsWith("\uFEFF")) {
                header = header.substring(1);
            }

            if (header.isBlank()) {
                throw new CsvParseException(
                        "CSV_PARSE_ERROR: Header names cannot be blank"
                );
            }

            if (!seenHeaders.add(header)) {
                throw new CsvParseException(
                        "CSV_PARSE_ERROR: Duplicate header: " + header
                );
            }

            headers.add(header);
        }

        return List.copyOf(headers);
    }

    private static void stripUtf8Bom(BufferedInputStream input)
            throws IOException {

        input.mark(3);

        int first = input.read();
        int second = input.read();
        int third = input.read();

        boolean hasBom = first == 0xEF
                && second == 0xBB
                && third == 0xBF;

        if (!hasBom) {
            input.reset();
        }
    }

    public static class CsvParseException extends IOException {

        private static final long serialVersionUID = 1L;

        public CsvParseException(String message) {
            super(message);
        }

        public CsvParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CsvLimitException extends IOException {

        private static final long serialVersionUID = 1L;

        public CsvLimitException(String message) {
            super(message);
        }
    }
}