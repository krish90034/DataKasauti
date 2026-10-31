package com.datakasauti.engine.clean;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Writes cleaned CSV as UTF-8 without BOM, LF separators and minimal quoting. */
public final class CsvWriter {
    public void write(Path destination, List<String> headers, List<List<String>> rows) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setRecordSeparator("\n").get();
        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            List<String> safeHeaders = headers.stream().map(this::protectFormulaInjection).toList();
            printer.printRecord(safeHeaders);
            for (List<String> row : rows) {
                List<String> safeValues = new ArrayList<>(row.size());
                for (String value : row) safeValues.add(protectFormulaInjection(value));
                printer.printRecord(safeValues);
            }
        }
    }

    private String protectFormulaInjection(String value) {
        if (value == null || value.isEmpty()) return value;
        char first = value.charAt(0);
        if (first != '=' && first != '+' && first != '-' && first != '@') return value;
        try {
            new BigDecimal(value.strip());
            return value;
        } catch (NumberFormatException exception) {
            return "'" + value;
        }
    }
}
