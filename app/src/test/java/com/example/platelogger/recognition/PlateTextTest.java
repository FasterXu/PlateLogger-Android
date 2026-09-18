package com.example.platelogger.recognition;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlateTextTest {
    @Test public void normalizesCommonSeparators() {
        assertEquals("京A12345", PlateText.normalize(" 京a·12345 "));
    }

    @Test public void acceptsStandardAndNewEnergyPlates() {
        assertTrue(PlateText.isLikelyChinesePlate("京A12345"));
        assertTrue(PlateText.isLikelyChinesePlate("粤BD12345"));
    }

    @Test public void acceptsSpecialAndDiplomaticPlates() {
        assertTrue(PlateText.isLikelyChinesePlate("京A1234警"));
        assertTrue(PlateText.isLikelyChinesePlate("粤Z1234港"));
        assertTrue(PlateText.isLikelyChinesePlate("224001使"));
    }

    @Test public void rejectsAmbiguousOrMalformedText() {
        assertFalse(PlateText.isLikelyChinesePlate("京I12345"));
        assertFalse(PlateText.isLikelyChinesePlate("ABC123"));
    }
}
