package com.example.platelogger.recognition;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CapturePolicyTest {
    @Test public void firstAppearanceIsRecorded() {
        assertTrue(CapturePolicy.shouldRecord(0L, 1_000L));
    }

    @Test public void samePlateMustWaitFullMinute() {
        long previous = 1_000_000L;
        assertFalse(CapturePolicy.shouldRecord(previous, previous + 59_999L));
        assertTrue(CapturePolicy.shouldRecord(previous, previous + 60_000L));
    }
}
