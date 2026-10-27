package com.datakasauti.engine.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvReaderTest {

    @TempDir
    Path tempDir;

    private Path createCsv(String filename, String content)
            throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void readsRowsUsingHeaderNames() throws IOException {
        Path file = createCsv(
                "people.csv",
                "name,age,city\nAbhishek,21,Lucknow\nRiya,22,Delhi\n"
        );

        List<Map<String, String>> rows = new CsvReader().read(file);

        assertEquals(2, rows.size());
        assertEquals("Abhishek", rows.get(0).get("name"));
        assertEquals("21", rows.get(0).get("age"));
        assertEquals("Lucknow", rows.get(0).get("city"));
        assertEquals("Riya", rows.get(1).get("name"));
    }

    @Test
    void readsQuotedValuesContainingCommas() throws IOException {
        Path file = createCsv(
                "quoted.csv",
                "name,description\nAbhishek,\"Designer, Developer\"\n"
        );

        List<Map<String, String>> rows = new CsvReader().read(file);

        assertEquals(
                "Designer, Developer",
                rows.get(0).get("description")
        );
    }

    @Test
    void preservesEmptyCellsAsEmptyStrings() throws IOException {
        Path file = createCsv(
                "empty.csv",
                "name,city\nAbhishek,\n"
        );

        List<Map<String, String>> rows = new CsvReader().read(file);

        assertEquals("", rows.get(0).get("city"));
    }
}