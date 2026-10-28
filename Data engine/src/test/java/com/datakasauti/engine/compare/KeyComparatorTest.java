package com.datakasauti.engine.compare;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeyComparatorTest {

    @Test
    void countsAddedRemovedUnchangedAndChangedRows() {
        List<String> headers = List.of("id", "name", "amount");

        List<Map<String, String>> baseline = List.of(
                Map.of("id", "1", "name", "Alice", "amount", "10"),
                Map.of("id", "2", "name", "Bob", "amount", "20"),
                Map.of("id", "3", "name", "Cara", "amount", "30")
        );

        List<Map<String, String>> current = List.of(
                Map.of("id", "1", "name", "Alice", "amount", "10"),
                Map.of("id", "2", "name", "Bob", "amount", "25"),
                Map.of("id", "4", "name", "Dave", "amount", "40")
        );

        KeyComparator.Result result = new KeyComparator().compare(
                headers, baseline, headers, current, "id"
        );

        assertTrue(result.warnings().isEmpty());
        assertNotNull(result.rowChanges());

        assertEquals(1, result.rowChanges().addedKeysCount());
        assertEquals(1, result.rowChanges().removedKeysCount());
        assertEquals(1, result.rowChanges().unchangedKeysCount());
        assertEquals(1, result.rowChanges().changedKeysCount());
    }

    @Test
    void rejectsMissingKeyValues() {
        List<String> headers = List.of("id", "name");

        List<Map<String, String>> baseline = List.of(
                Map.of("id", "1", "name", "Alice"),
                Map.of("id", "", "name", "Bob")
        );

        List<Map<String, String>> current = List.of(
                Map.of("id", "1", "name", "Alice")
        );

        KeyComparator.Result result = new KeyComparator().compare(
                headers, baseline, headers, current, "id"
        );

        assertNull(result.rowChanges());
        assertTrue(result.warnings().get(0).contains("KEY_NOT_UNIQUE"));
    }

    @Test
    void rejectsDuplicateKeys() {
        List<String> headers = List.of("id", "name");

        List<Map<String, String>> baseline = List.of(
                Map.of("id", "1", "name", "Alice"),
                Map.of("id", "1", "name", "Bob")
        );

        List<Map<String, String>> current = List.of(
                Map.of("id", "1", "name", "Alice")
        );

        KeyComparator.Result result = new KeyComparator().compare(
                headers, baseline, headers, current, "id"
        );

        assertNull(result.rowChanges());
        assertTrue(result.warnings().get(0).contains("KEY_NOT_UNIQUE"));
    }

    @Test
    void reportsWhenKeyColumnIsMissing() {
        List<String> baselineHeaders = List.of("id", "name");
        List<String> currentHeaders = List.of("name");

        KeyComparator.Result result = new KeyComparator().compare(
                baselineHeaders,
                List.of(Map.of("id", "1", "name", "Alice")),
                currentHeaders,
                List.of(Map.of("name", "Alice")),
                "id"
        );

        assertNull(result.rowChanges());
        assertTrue(
                result.warnings().get(0).contains("KEY_COLUMN_NOT_FOUND")
        );
    }
}