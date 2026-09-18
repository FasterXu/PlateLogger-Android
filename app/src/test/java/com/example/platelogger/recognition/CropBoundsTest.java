package com.example.platelogger.recognition;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CropBoundsTest {
    @Test public void doublesDetectedWidthAndHeightAroundCenter() {
        CropBounds.Bounds bounds = CropBounds.doubled(100f, 80f, 300f, 180f,
                800, 600);
        assertEquals(400, bounds.width());
        assertEquals(200, bounds.height());
        assertEquals(0, bounds.left);
        assertEquals(30, bounds.top);
    }

    @Test public void clampsExpandedCropToImageEdges() {
        CropBounds.Bounds bounds = CropBounds.doubled(10f, 10f, 110f, 60f,
                160, 90);
        assertEquals(0, bounds.left);
        assertEquals(0, bounds.top);
        assertEquals(160, bounds.right);
        assertEquals(85, bounds.bottom);
    }
}
