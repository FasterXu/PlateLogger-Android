package com.example.platelogger.ui;

import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import com.example.platelogger.recognition.PlateVisualStyle;

/** Applies an isolated drawable so recycled list rows never share another plate's color. */
public final class PlateBadgeStyler {
    private PlateBadgeStyler() { }

    public static void apply(TextView badge, String plateType) {
        PlateVisualStyle.Style style = PlateVisualStyle.forType(plateType);
        float density = badge.getResources().getDisplayMetrics().density;
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(style.backgroundColor);
        background.setCornerRadius(6f * density);
        background.setStroke(Math.max(1, Math.round(density)), style.borderColor);
        badge.setBackground(background);
        badge.setTextColor(style.textColor);
        badge.setPadding(Math.round(10f * density), Math.round(5f * density),
                Math.round(10f * density), Math.round(5f * density));
    }
}
