package com.example.platelogger.recognition;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlateCategoryTest {
    @Test public void distinguishesEnergyAndLargeVehicleTypes() {
        assertEquals("新能源车牌", PlateCategory.labelFor("粤BD12345", 3));
        assertEquals("大型车辆/货车（双层黄牌）", PlateCategory.labelFor("沪A12345", 9));
    }

    @Test public void specialTextOverridesColorType() {
        assertEquals("使馆车牌", PlateCategory.labelFor("224001使", 4));
        assertEquals("警用车牌", PlateCategory.labelFor("京A1234警", 2));
        assertEquals("香港入出境车牌", PlateCategory.labelFor("粤Z1234港", 0));
    }
}
