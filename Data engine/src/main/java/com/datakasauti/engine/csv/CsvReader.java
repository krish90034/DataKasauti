package com.datakasauti.engine.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvReader {

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .get();

    public List<Map<String, String>> read(Path path) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(
                    path, StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, CSV_FORMAT)) {

            List<String> headers = parser.getHeaderNames();

            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();

                for (String header : headers) {
                    row.put(header, record.get(header));
                }

                rows.add(row);
            }
        }

        return rows;
    }
}