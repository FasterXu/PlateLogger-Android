package com.example.platelogger.recognition;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PlateText {
    private static final Pattern MAINLAND_PLATE = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-HJ-NP-Z][A-HJ-NP-Z0-9]{5,6}$");
    private static final Pattern SPECIAL_PLATE = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,5}[警学挂港澳]$");
    private static final Pattern DIPLOMATIC_PLATE = Pattern.compile(
            "^(?:[使领][A-HJ-NP-Z0-9]{5,7}|[A-HJ-NP-Z0-9]{5,7}[使领])$");

    private PlateText() { }

    public static String normalize(String input) {
        if (input == null) return "";
        return input.toUpperCase(Locale.ROOT)
                .replace(" ", "")
                .replace("·", "")
                .replace(".", "")
                .trim();
    }

    public static boolean isLikelyChinesePlate(String value) {
        String normalized = normalize(value);
        return MAINLAND_PLATE.matcher(normalized).matches()
                || SPECIAL_PLATE.matcher(normalized).matches()
                || DIPLOMATIC_PLATE.matcher(normalized).matches();
    }
}
