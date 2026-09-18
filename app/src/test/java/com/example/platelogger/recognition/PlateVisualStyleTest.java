package com.example.platelogger.recognition;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlateVisualStyleTest {
    @Test public void usesExpectedColorsForCommonPlateTypes() {
        assertEquals(PlateVisualStyle.BLUE_BACKGROUND,
                PlateVisualStyle.forType("普通蓝牌").backgroundColor);
        assertEquals(PlateVisualStyle.YELLOW_BACKGROUND,
                PlateVisualStyle.forType("大型车辆/货车（双层黄牌）").backgroundColor);
        assertEquals(PlateVisualStyle.GREEN_BACKGROUND,
                PlateVisualStyle.forType("新能源车牌").backgroundColor);
    }

    @Test public void usesSpecialPlateColorsAndReadableText() {
        assertEquals(PlateVisualStyle.BLACK_BACKGROUND,
                PlateVisualStyle.forType("使馆车牌").backgroundColor);
        assertEquals(PlateVisualStyle.WHITE_BACKGROUND,
                PlateVisualStyle.forType("警用车牌").backgroundColor);
        assertEquals(PlateVisualStyle.DARK_TEXT,
                PlateVisualStyle.forType("大型车辆等（单层黄牌）").textColor);
    }
}
