package com.datakasauti.engine.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesUtf8CsvWithLfAndFormulaProtection() throws Exception {
        Path output = tempDir.resolve("cleaned.csv");

        Map<String, String> row = new LinkedHashMap<>();
        row.put("name", "Alice");
        row.put("formula", "=1+1");
        row.put("amount", "-5");
        row.put("city", "Lucknow, UP");

        new CsvWriter().write(
                output,
                List.of("name", "formula", "amount", "city"),
                List.of(row)
        );

        String content = Files.readString(output, StandardCharsets.UTF_8);

        assertEquals(
                "name,formula,amount,city\n"
                        + "Alice,'=1+1,-5,\"Lucknow, UP\"\n",
                content
        );

        assertFalse(content.startsWith("\uFEFF"));
        assertFalse(content.contains("\r"));
    }

    @Test
    void preservesCommasAndQuotesInsideValues() throws Exception {
        Path output = tempDir.resolve("quoted.csv");

        Map<String, String> row = new LinkedHashMap<>();
        row.put("description", "He said \"hello\", friend");

        new CsvWriter().write(
                output,
                List.of("description"),
                List.of(row)
        );

        String content = Files.readString(output, StandardCharsets.UTF_8);

        try (CSVParser parser = CSVParser.parse(
                content, CSVFormat.DEFAULT)) {

            var records = parser.getRecords();

            assertEquals(2, records.size());
            assertEquals(
                    "He said \"hello\", friend",
                    records.get(1).get(0)
            );
        }
    }

    @Test
    void writesEmptyValuesAsEmptyCells() throws Exception {
        Path output = tempDir.resolve("empty.csv");

        Map<String, String> row = new LinkedHashMap<>();
        row.put("name", "Abhishek");
        row.put("city", "");

        new CsvWriter().write(
                output,
                List.of("name", "city"),
                List.of(row)
        );

        String content = Files.readString(output, StandardCharsets.UTF_8);

        assertEquals("name,city\nAbhishek,\n", content);
    }
}