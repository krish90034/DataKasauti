package com.datakasauti.engine.profile;

import com.datakasauti.engine.csv.ColumnType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfilerTest {

    @TempDir
    Path tempDir;

    private Path createCsv(String name, String content) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void calculatesRowsMissingValuesAndStatistics() throws Exception {
        Path file = createCsv(
                "sales.csv",
                "name,score,tag\n"
                        + "Alice,10,A\n"
                        + "Bob,20,B\n"
                        + "Alice,10,A\n"
                        + ",30,C\n"
        );

        ProfileResult result = new Profiler().profile(file);

        assertEquals(4, result.rowCount());
        assertEquals(3, result.columnCount());
        assertEquals(1, result.duplicateRows());
        assertEquals(1, result.totalMissingValues());
        assertEquals(Map.of("name", 1L), result.missingValues());

        ProfileResult.ColumnSummary score = result.columns().get(1);

        assertEquals(ColumnType.INTEGER, score.detectedType());
        assertEquals(3, score.uniqueCount());
        assertEquals(10.0, score.stats().min());
        assertEquals(30.0, score.stats().max());
        assertEquals(17.5, score.stats().mean());
        assertEquals(15.0, score.stats().median());
    }

    @Test
    void countsEveryRepeatedRowAfterTheFirst() throws Exception {
        Path file = createCsv(
                "duplicates.csv",
                "name,city\nAlice,Lucknow\nAlice,Lucknow\nAlice,Lucknow\n"
        );

        ProfileResult result = new Profiler().profile(file);
        assertEquals(2, result.duplicateRows());
    }

    @Test
    void countsDistinctRawValuesAndKeepsNaAsAValue() throws Exception {
        Path file = createCsv(
                "values.csv",
                "name,code\nAlice,NA\n Alice ,null\nAlice,NaN\n"
        );

        ProfileResult result = new Profiler().profile(file);

        assertEquals(0, result.totalMissingValues());
        assertTrue(result.missingValues().isEmpty());
        assertEquals(2, result.columns().get(0).uniqueCount());
        assertEquals(3, result.columns().get(1).uniqueCount());
    }

    @Test
    void previewContainsAtMostTwentyRows() throws Exception {
        StringBuilder csv = new StringBuilder("id,value\n");

        for (int i = 1; i <= 25; i++) {
            csv.append(i).append(",").append(i * 10).append("\n");
        }

        Path file = createCsv("preview.csv", csv.toString());
        ProfileResult result = new Profiler().profile(file);

        assertEquals(25, result.rowCount());
        assertEquals(20, result.previewRows().size());
        assertEquals(List.of("1", "10"), result.previewRows().get(0));
        assertEquals(List.of("20", "200"), result.previewRows().get(19));
    }

    @Test
    void handlesCsvWithHeadersButNoDataRows() throws Exception {
        Path file = createCsv("empty-data.csv", "name,score\n");
        ProfileResult result = new Profiler().profile(file);

        assertEquals(0, result.rowCount());
        assertEquals(2, result.columnCount());
        assertEquals(0, result.totalMissingValues());
        assertTrue(result.previewRows().isEmpty());
        assertEquals(ColumnType.STRING, result.columns().get(0).detectedType());
        assertNull(result.columns().get(0).stats());
    }

    @Test
    void roundsMeanAndMedianToTwoDecimalPlaces() throws Exception {
        Path file = createCsv("rounding.csv", "price\n1.00\n1.01\n");
        ProfileResult result = new Profiler().profile(file);

        ProfileResult.NumericStats stats = result.columns().get(0).stats();

        assertEquals(1.01, stats.mean(), 0.0001);
        assertEquals(1.01, stats.median(), 0.0001);
    }

    @Test
    void preservesColumnOrderFromCsvHeader() throws Exception {
        Path file = createCsv("order.csv", "z,a,m\n1,2,3\n");
        ProfileResult result = new Profiler().profile(file);

        assertEquals(
                List.of("z", "a", "m"),
                result.columns().stream()
                        .map(ProfileResult.ColumnSummary::name)
                        .toList()
        );
    }
}