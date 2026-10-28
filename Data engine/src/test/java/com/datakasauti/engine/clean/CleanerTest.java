package com.datakasauti.engine.clean;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CleanerTest {

    private final Cleaner cleaner = new Cleaner();

    @Test
    void trimsWhitespaceBeforeRemovingDuplicates() {
        List<String> columns = List.of("name", "city");

        List<Map<String, String>> rows = List.of(
                Map.of("name", "A ", "city", " X "),
                Map.of("name", "A", "city", "X"),
                Map.of("name", "B", "city", "Y")
        );

        CleanPreviewResult result = cleaner.clean(
                columns,
                rows,
                List.of(
                        CleaningRule.removeDuplicates(),
                        CleaningRule.trimWhitespace()
                )
        );

        assertEquals(3, result.rowsBefore());
        assertEquals(2, result.rowsAfter());
        assertFalse(result.originalModified());

        assertEquals("A", result.cleanedRows().get(0).get("name"));
        assertEquals("X", result.cleanedRows().get(0).get("city"));

        assertEquals(
                RuleType.TRIM_WHITESPACE,
                result.ruleResults().get(0).type()
        );

        assertEquals(
                RuleType.REMOVE_DUPLICATES,
                result.ruleResults().get(1).type()
        );

        assertEquals(1, result.ruleResults().get(1).rowsAffected());
    }

    @Test
    void fillsMissingNumericValuesUsingMean() {
        List<String> columns = List.of("score");

        List<Map<String, String>> rows = List.of(
                Map.of("score", "10"),
                Map.of("score", ""),
                Map.of("score", "20")
        );

        CleanPreviewResult result = cleaner.clean(
                columns,
                rows,
                List.of(
                        CleaningRule.fillMissing(
                                "score", FillStrategy.MEAN, null
                        )
                )
        );

        assertEquals(3, result.rowsAfter());
        assertEquals("15", result.cleanedRows().get(1).get("score"));
        assertEquals(1, result.ruleResults().get(0).cellsChanged());
    }

    @Test
    void modeUsesFirstValueWhenFrequenciesTie() {
        List<String> columns = List.of("category");

        List<Map<String, String>> rows = List.of(
                Map.of("category", "A"),
                Map.of("category", "B"),
                Map.of("category", "A"),
                Map.of("category", "B"),
                Map.of("category", "")
        );

        CleanPreviewResult result = cleaner.clean(
                columns,
                rows,
                List.of(
                        CleaningRule.fillMissing(
                                "category", FillStrategy.MODE, null
                        )
                )
        );

        assertEquals(
                "A",
                result.cleanedRows().get(4).get("category")
        );
    }

    @Test
    void dropsRowsWithMissingValuesInSelectedColumns() {
        List<String> columns = List.of("id", "amount");

        List<Map<String, String>> rows = List.of(
                Map.of("id", "1", "amount", "10"),
                Map.of("id", "2", "amount", ""),
                Map.of("id", "3", "amount", "30")
        );

        CleanPreviewResult result = cleaner.clean(
                columns,
                rows,
                List.of(
                        CleaningRule.dropRowsWithMissing(
                                List.of("amount")
                        )
                )
        );

        assertEquals(3, result.rowsBefore());
        assertEquals(2, result.rowsAfter());
        assertEquals("1", result.cleanedRows().get(0).get("id"));
        assertEquals("3", result.cleanedRows().get(1).get("id"));
        assertEquals(1, result.removedRowsSample().size());
    }

    @Test
    void rejectsConstantFillWithoutValue() {
        List<String> columns = List.of("name");

        List<Map<String, String>> rows = List.of(
                Map.of("name", "")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> cleaner.clean(
                        columns,
                        rows,
                        List.of(
                                CleaningRule.fillMissing(
                                        "name",
                                        FillStrategy.CONSTANT,
                                        null
                                )
                        )
                )
        );
    }
}