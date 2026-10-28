package com.datakasauti.engine.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CsvWriter {

    private static final CSVFormat CSV_FORMAT =
            CSVFormat.DEFAULT.builder()
                    .setRecordSeparator("\n")
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .setIgnoreEmptyLines(false)
                    .get();

    public void write(
            Path outputPath,
            List<String> columns,
            List<Map<String, String>> rows) throws IOException {

        Objects.requireNonNull(outputPath, "outputPath must not be null");

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            writeRecords(writer, columns, rows);
        }
    }

    public void write(
            OutputStream outputStream,
            List<String> columns,
            List<Map<String, String>> rows) throws IOException {

        Objects.requireNonNull(
                outputStream, "outputStream must not be null");

        OutputStreamWriter writer = new OutputStreamWriter(
                outputStream, StandardCharsets.UTF_8);

        writeRecords(writer, columns, rows);
    }

    private void writeRecords(
            Appendable output,
            List<String> columns,
            List<Map<String, String>> rows) throws IOException {

        Objects.requireNonNull(columns, "columns must not be null");
        Objects.requireNonNull(rows, "rows must not be null");

        try (CSVPrinter printer = new CSVPrinter(output, CSV_FORMAT)) {

            printer.printRecord(safeRecord(columns));

            for (Map<String, String> row : rows) {
                List<String> values = new ArrayList<>(columns.size());

                for (String column : columns) {
                    values.add(safeCell(row.get(column)));
                }

                printer.printRecord(values);
            }

            printer.flush();
        }
    }

    private List<String> safeRecord(List<String> values) {
        List<String> safeValues = new ArrayList<>(values.size());

        for (String value : values) {
            safeValues.add(safeCell(value));
        }

        return safeValues;
    }

    private String safeCell(String value) {
        if (value == null) {
            return "";
        }

        if (value.isEmpty()) {
            return value;
        }

        char first = value.charAt(0);

        boolean dangerous = first == '='
                || first == '+'
                || first == '-'
                || first == '@';

        if (!dangerous || isValidNumber(value)) {
            return value;
        }

        return "'" + value;
    }

    private boolean isValidNumber(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}