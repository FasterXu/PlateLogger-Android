package com.example.platelogger.recognition;

public final class CapturePolicy {
    public static final long MIN_SAME_PLATE_INTERVAL_MS = 60_000L;

    private CapturePolicy() { }

    public static boolean shouldRecord(long latestTimestamp, long currentTimestamp) {
        return latestTimestamp <= 0L
                || currentTimestamp - latestTimestamp >= MIN_SAME_PLATE_INTERVAL_MS;
    }
}
