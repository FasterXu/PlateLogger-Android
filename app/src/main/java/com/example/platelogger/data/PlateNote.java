package com.example.platelogger.data;

public final class PlateNote {
    public static final int MAX_CODE_POINTS = 80;

    private PlateNote() {
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String normalized = value.trim().replaceAll("\\s+", " ");
        int count = normalized.codePointCount(0, normalized.length());
        if (count <= MAX_CODE_POINTS) return normalized;
        int end = normalized.offsetByCodePoints(0, MAX_CODE_POINTS);
        return normalized.substring(0, end);
    }
}
