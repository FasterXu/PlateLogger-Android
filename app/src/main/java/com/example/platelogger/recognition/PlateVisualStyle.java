package com.example.platelogger.recognition;

/** Color palette for a plate badge, selected from the recognized plate category. */
public final class PlateVisualStyle {
    public static final int BLUE_BACKGROUND = 0xFF0D47A1;
    public static final int YELLOW_BACKGROUND = 0xFFFBC02D;
    public static final int GREEN_BACKGROUND = 0xFF43A047;
    public static final int WHITE_BACKGROUND = 0xFFFAFAFA;
    public static final int BLACK_BACKGROUND = 0xFF151515;
    public static final int UNKNOWN_BACKGROUND = 0xFF546E7A;
    public static final int LIGHT_TEXT = 0xFFFFFFFF;
    public static final int DARK_TEXT = 0xFF111111;

    private PlateVisualStyle() { }

    public static Style forType(String plateType) {
        String type = plateType == null ? "" : plateType;
        if (type.contains("新能源")) {
            return new Style(GREEN_BACKGROUND, DARK_TEXT, 0xFF1B5E20);
        }
        if (type.contains("黄牌") || type.contains("大型车辆")
                || type.contains("货车") || type.contains("教练")
                || type.contains("挂车")) {
            return new Style(YELLOW_BACKGROUND, DARK_TEXT, 0xFF8D6E00);
        }
        if (type.contains("使馆") || type.contains("领馆")
                || type.contains("黑色") || type.contains("入出境")) {
            return new Style(BLACK_BACKGROUND, LIGHT_TEXT, 0xFFBDBDBD);
        }
        if (type.contains("白色") || type.contains("警用")
                || type.contains("香港") || type.contains("澳门")) {
            return new Style(WHITE_BACKGROUND, DARK_TEXT, 0xFF616161);
        }
        if (type.contains("蓝牌")) {
            return new Style(BLUE_BACKGROUND, LIGHT_TEXT, 0xFFFFFFFF);
        }
        return new Style(UNKNOWN_BACKGROUND, LIGHT_TEXT, 0xFFCFD8DC);
    }

    public static final class Style {
        public final int backgroundColor;
        public final int textColor;
        public final int borderColor;

        Style(int backgroundColor, int textColor, int borderColor) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.borderColor = borderColor;
        }
    }
}
