package com.example.platelogger.recognition;

/** Calculates an image crop whose width and height are twice the detected plate box. */
public final class CropBounds {
    private CropBounds() { }

    public static Bounds doubled(float x1, float y1, float x2, float y2,
                                 int imageWidth, int imageHeight) {
        if (imageWidth < 1 || imageHeight < 1) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        float detectedWidth = Math.max(1f, x2 - x1);
        float detectedHeight = Math.max(1f, y2 - y1);
        float centerX = (x1 + x2) / 2f;
        float centerY = (y1 + y2) / 2f;

        int left = clamp(Math.round(centerX - detectedWidth), 0, imageWidth - 1);
        int top = clamp(Math.round(centerY - detectedHeight), 0, imageHeight - 1);
        int right = clamp(Math.round(centerX + detectedWidth), left + 1, imageWidth);
        int bottom = clamp(Math.round(centerY + detectedHeight), top + 1, imageHeight);
        return new Bounds(left, top, right, bottom);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class Bounds {
        public final int left;
        public final int top;
        public final int right;
        public final int bottom;

        Bounds(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }
    }
}
