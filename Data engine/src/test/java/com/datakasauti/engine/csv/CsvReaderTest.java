package com.datakasauti.engine.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvReaderTest {

    @TempDir
    Path tempDir;

    private Path createCsv(String filename, String content)
            throws IOException {

        Path file = tempDir.resolve(filename);

        Files.writeString(
                file,
                content,
                StandardCharsets.UTF_8
        );

        return file;
    }

    @Test
    void readsRowsUsingHeaderNames() throws IOException {
        Path file = createCsv(
                "people.csv",
                "name,age,city\n"
                        + "Abhishek,21,Lucknow\n"
                        + "Riya,22,Delhi\n"
        );

        List<Map<String, String>> rows =
                new CsvReader().read(file);

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
                "name,description\n"
                        + "Abhishek,\"Designer, Developer\"\n"
        );

        List<Map<String, String>> rows =
                new CsvReader().read(file);

        assertEquals(
                "Designer, Developer",
                rows.get(0).get("description")
        );
    }

    @Test
    void readsEmbeddedNewlines() throws IOException {
        Path file = createCsv(
                "multiline.csv",
                "notes\n\"line one\nline two\"\n"
        );

        List<Map<String, String>> rows =
                new CsvReader().read(file);

        assertEquals(
                "line one\nline two",
                rows.get(0).get("notes")
        );
    }

    @Test
    void removesUtf8Bom() throws IOException {
        Path file = tempDir.resolve("bom.csv");

        byte[] content = (
                "\uFEFFname,age\nAbhishek,21\n"
        ).getBytes(StandardCharsets.UTF_8);

        Files.write(file, content);

        List<Map<String, String>> rows =
                new CsvReader().read(file);

        assertEquals("Abhishek", rows.get(0).get("name"));
        assertEquals(2, rows.get(0).size());
    }

    @Test
    void readsHeaderSeparately() throws IOException {
        byte[] content = "name,age,city\nAbhishek,21,Lucknow\n"
                .getBytes(StandardCharsets.UTF_8);

        List<String> headers = CsvReader.readHeader(
                new ByteArrayInputStream(content)
        );

        assertEquals(
                List.of("name", "age", "city"),
                headers
        );
    }

    @Test
    void preservesEmptyCellsAsEmptyStrings() throws IOException {
        Path file = createCsv(
                "empty.csv",
                "name,city\nAbhishek,\n"
        );

        List<Map<String, String>> rows =
                new CsvReader().read(file);

        assertEquals("", rows.get(0).get("city"));
    }

    @Test
    void padsShortRowsAtFivePercent() throws IOException {
        StringBuilder csv = new StringBuilder("name,city\n");
        csv.append("Abhishek\n");

        for (int i = 1; i < 20; i++) {
            csv.append("User")
                    .append(i)
                    .append(",City")
                    .append(i)
                    .append("\n");
        }

        Path file = createCsv("short-allowed.csv", csv.toString());

        List<Map<String, String>> rows =
                new CsvReader().read(file);

        assertEquals(20, rows.size());
        assertEquals("", rows.get(0).get("city"));
    }

    @Test
    void rejectsMoreThanFivePercentShortRows() throws IOException {
        StringBuilder csv = new StringBuilder("name,city\n");
        csv.append("Abhishek\n");
        csv.append("Riya\n");

        for (int i = 2; i < 20; i++) {
            csv.append("User")
                    .append(i)
                    .append(",City")
                    .append(i)
                    .append("\n");
        }

        Path file = createCsv("short-rejected.csv", csv.toString());

        assertThrows(
                CsvReader.CsvParseException.class,
                () -> new CsvReader().read(file)
        );
    }

    @Test
    void rejectsLongRows() throws IOException {
        Path file = createCsv(
                "long.csv",
                "name,city\nAbhishek,Lucknow,Extra\n"
        );

        assertThrows(
                CsvReader.CsvParseException.class,
                () -> new CsvReader().read(file)
        );
    }

    @Test
    void rejectsDuplicateHeaders() {
        byte[] content = "name,name\nAbhishek,Gond\n"
                .getBytes(StandardCharsets.UTF_8);

        assertThrows(
                CsvReader.CsvParseException.class,
                () -> CsvReader.readHeader(
                        new ByteArrayInputStream(content)
                )
        );
    }

    @Test
    void rejectsMoreThan200Columns() {
        String header = String.join(
                ",",
                IntStream.range(0, 201)
                        .mapToObj(i -> "column" + i)
                        .toList()
        );

        byte[] content = (header + "\n")
                .getBytes(StandardCharsets.UTF_8);

        assertThrows(
                CsvReader.CsvLimitException.class,
                () -> CsvReader.readHeader(
                        new ByteArrayInputStream(content)
                )
        );
    }
}