package com.datakasauti.engine.compare;

import com.datakasauti.engine.csv.ColumnType;
import com.datakasauti.engine.profile.ProfileResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaComparatorTest {

    private ProfileResult.ColumnSummary column(
            String name,
            ColumnType type) {
        return new ProfileResult.ColumnSummary(
                name, type, 0, 0.0, 5, null
        );
    }

    @Test
    void findsAddedRemovedAndCommonColumns() {
        List<ProfileResult.ColumnSummary> baseline = List.of(
                column("id", ColumnType.INTEGER),
                column("name", ColumnType.STRING),
                column("sales", ColumnType.DECIMAL)
        );

        List<ProfileResult.ColumnSummary> current = List.of(
                column("id", ColumnType.INTEGER),
                column("sales", ColumnType.DECIMAL),
                column("region", ColumnType.STRING)
        );

        CompareResult.SchemaResult result =
                new SchemaComparator().compare(baseline, current);

        assertEquals(List.of("region"), result.addedColumns());
        assertEquals(List.of("name"), result.removedColumns());
        assertEquals(
                List.of("id", "sales"),
                result.commonColumns()
        );
        assertTrue(result.typeChanges().isEmpty());
    }

    @Test
    void detectsColumnTypeChanges() {
        List<ProfileResult.ColumnSummary> baseline = List.of(
                column("amount", ColumnType.INTEGER)
        );

        List<ProfileResult.ColumnSummary> current = List.of(
                column("amount", ColumnType.DECIMAL)
        );

        CompareResult.SchemaResult result =
                new SchemaComparator().compare(baseline, current);

        assertEquals(1, result.typeChanges().size());

        CompareResult.TypeChange change =
                result.typeChanges().get(0);

        assertEquals("amount", change.column());
        assertEquals(ColumnType.INTEGER, change.baselineType());
        assertEquals(ColumnType.DECIMAL, change.currentType());
    }

    @Test
    void treatsColumnNamesAsCaseSensitive() {
        List<ProfileResult.ColumnSummary> baseline = List.of(
                column("Name", ColumnType.STRING)
        );

        List<ProfileResult.ColumnSummary> current = List.of(
                column("name", ColumnType.STRING)
        );

        CompareResult.SchemaResult result =
                new SchemaComparator().compare(baseline, current);

        assertEquals(List.of("name"), result.addedColumns());
        assertEquals(List.of("Name"), result.removedColumns());
        assertTrue(result.commonColumns().isEmpty());
    }

    @Test
    void handlesEmptySchemas() {
        CompareResult.SchemaResult result =
                new SchemaComparator().compare(List.of(), List.of());

        assertTrue(result.addedColumns().isEmpty());
        assertTrue(result.removedColumns().isEmpty());
        assertTrue(result.commonColumns().isEmpty());
        assertTrue(result.typeChanges().isEmpty());
    }
}