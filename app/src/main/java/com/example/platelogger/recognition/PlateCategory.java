package com.example.platelogger.recognition;

/** Converts HyperLPR3's plate-type result and recognized text into a user-facing label. */
public final class PlateCategory {
    // Values defined by HyperLPR3 1.0.3 TypeDefine.
    private static final int TYPE_BLUE = 0;
    private static final int TYPE_YELLOW_SINGLE = 1;
    private static final int TYPE_WHITE_SINGLE = 2;
    private static final int TYPE_GREEN = 3;
    private static final int TYPE_BLACK_HK_MACAO = 4;
    private static final int TYPE_HK_SINGLE = 5;
    private static final int TYPE_HK_DOUBLE = 6;
    private static final int TYPE_MACAO_SINGLE = 7;
    private static final int TYPE_MACAO_DOUBLE = 8;
    private static final int TYPE_YELLOW_DOUBLE = 9;

    private PlateCategory() { }

    public static String labelFor(String plateNumber, int sdkType) {
        String code = PlateText.normalize(plateNumber);
        if (code.contains("使")) return "使馆车牌";
        if (code.contains("领")) return "领馆车牌";
        if (code.endsWith("警")) return "警用车牌";
        if (code.endsWith("学")) return "教练车牌";
        if (code.endsWith("挂")) return "挂车车牌";
        if (code.endsWith("港")) return "香港入出境车牌";
        if (code.endsWith("澳")) return "澳门入出境车牌";

        switch (sdkType) {
            case TYPE_BLUE:
                return "普通蓝牌";
            case TYPE_YELLOW_SINGLE:
                return "大型车辆等（单层黄牌）";
            case TYPE_WHITE_SINGLE:
                return "白色特殊车牌";
            case TYPE_GREEN:
                return "新能源车牌";
            case TYPE_BLACK_HK_MACAO:
                return "黑色港澳车牌";
            case TYPE_HK_SINGLE:
                return "香港单层车牌";
            case TYPE_HK_DOUBLE:
                return "香港双层车牌";
            case TYPE_MACAO_SINGLE:
                return "澳门单层车牌";
            case TYPE_MACAO_DOUBLE:
                return "澳门双层车牌";
            case TYPE_YELLOW_DOUBLE:
                return "大型车辆/货车（双层黄牌）";
            default:
                return code.length() == 8 ? "新能源车牌" : "其他/未分类车牌";
        }
    }
}
