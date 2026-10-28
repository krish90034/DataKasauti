package com.datakasauti.engine.csv;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeInferrerTest {

    @Test
    void detectsInteger() {
        assertEquals(
                ColumnType.INTEGER,
                TypeInferrer.infer(List.of("10", "20", "-5"))
        );
    }

    @Test
    void detectsDecimal() {
        assertEquals(
                ColumnType.DECIMAL,
                TypeInferrer.infer(List.of("1.5", "2.75"))
        );
    }

    @Test
    void detectsIntegerBeyondLongRangeAsDecimal() {
        assertEquals(
                ColumnType.DECIMAL,
                TypeInferrer.infer(List.of("9223372036854775808"))
        );
    }

    @Test
    void detectsBooleanIgnoringCase() {
        assertEquals(
                ColumnType.BOOLEAN,
                TypeInferrer.infer(List.of("true", "FALSE", "True"))
        );
    }

    @Test
    void detectsIsoDates() {
        assertEquals(
                ColumnType.DATE,
                TypeInferrer.infer(
                        List.of("2024-02-29", "2025-12-31")
                )
        );
    }

    @Test
    void invalidDatesBecomeStrings() {
        assertEquals(
                ColumnType.STRING,
                TypeInferrer.infer(List.of("2024-02-30", "2025-03-01"))
        );
    }

    @Test
    void mixedValuesBecomeStrings() {
        assertEquals(
                ColumnType.STRING,
                TypeInferrer.infer(List.of("10", "hello"))
        );
    }

    @Test
    void allMissingValuesBecomeString() {
        assertEquals(
                ColumnType.STRING,
                TypeInferrer.infer(Arrays.asList("", " ", null))
        );
    }

    @Test
    void ignoresMissingValues() {
        assertEquals(
                ColumnType.INTEGER,
                TypeInferrer.infer(Arrays.asList("10", "", null, "20"))
        );
    }

    @Test
    void naNullAndNanAreNotMissing() {
        assertEquals(
                ColumnType.STRING,
                TypeInferrer.infer(List.of("1", "NA"))
        );

        assertEquals(
                ColumnType.STRING,
                TypeInferrer.infer(List.of("1", "null"))
        );

        assertEquals(
                ColumnType.STRING,
                TypeInferrer.infer(List.of("1", "NaN"))
        );
    }

    @Test
    void yesAndNoBecomeStrings() {
        assertEquals(
                ColumnType.STRING,
                TypeInferrer.infer(List.of("yes", "no"))
        );
    }
}