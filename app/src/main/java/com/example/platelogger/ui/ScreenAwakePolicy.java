package com.example.platelogger.ui;

public final class ScreenAwakePolicy {
    private ScreenAwakePolicy() {
    }

    public static boolean shouldKeepAwake(boolean activityResumed, boolean showingScanner,
                                          boolean keepScreenAwake) {
        return activityResumed && showingScanner && keepScreenAwake;
    }

    public static boolean shouldScheduleDim(boolean activityResumed, boolean showingScanner,
                                            boolean keepScreenAwake,
                                            boolean dimAfterInactivity) {
        return shouldKeepAwake(activityResumed, showingScanner, keepScreenAwake)
                && dimAfterInactivity;
    }
}
