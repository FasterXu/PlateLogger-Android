package com.example.platelogger.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ScreenAwakePolicyTest {
    @Test
    public void keepAwakeOnlyWhileScannerIsVisibleAndActivityResumed() {
        assertTrue(ScreenAwakePolicy.shouldKeepAwake(true, true, true));
        assertFalse(ScreenAwakePolicy.shouldKeepAwake(false, true, true));
        assertFalse(ScreenAwakePolicy.shouldKeepAwake(true, false, true));
        assertFalse(ScreenAwakePolicy.shouldKeepAwake(true, true, false));
    }

    @Test
    public void dimRequiresKeepAwakeAndDimSetting() {
        assertTrue(ScreenAwakePolicy.shouldScheduleDim(true, true, true, true));
        assertFalse(ScreenAwakePolicy.shouldScheduleDim(true, true, true, false));
        assertFalse(ScreenAwakePolicy.shouldScheduleDim(true, true, false, true));
        assertFalse(ScreenAwakePolicy.shouldScheduleDim(true, false, true, true));
    }
}
