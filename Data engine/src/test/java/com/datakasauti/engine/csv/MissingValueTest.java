package com.datakasauti.engine.csv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingValueTest {

    @Test
    void emptyStringIsMissing() {
        assertTrue(MissingValue.isMissing(""));
    }

    @Test
    void whitespaceIsMissing() {
        assertTrue(MissingValue.isMissing("   "));
    }

    @Test
    void nullIsMissing() {
        assertTrue(MissingValue.isMissing(null));
    }

    @Test
    void normalTextIsNotMissing() {
        assertFalse(MissingValue.isMissing("Abhishek"));
    }

    @Test
    void naIsNotMissing() {
        assertFalse(MissingValue.isMissing("NA"));
    }
}