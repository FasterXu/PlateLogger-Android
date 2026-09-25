package com.example.platelogger.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PlateNoteTest {
    @Test
    public void normalizesWhitespaceAndEmptyValues() {
        assertEquals("", PlateNote.normalize(null));
        assertEquals("", PlateNote.normalize("   \n  "));
        assertEquals("公司 访客车辆", PlateNote.normalize("  公司   访客车辆  "));
    }

    @Test
    public void limitsNoteToEightyUnicodeCodePoints() {
        String value = "😀".repeat(81);
        String normalized = PlateNote.normalize(value);
        assertEquals(80, normalized.codePointCount(0, normalized.length()));
    }
}
