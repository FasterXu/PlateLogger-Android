package com.example.platelogger.recognition;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public final class ImageConverter {
    private ImageConverter() { }

    /** ImageAnalysis must be configured with OUTPUT_IMAGE_FORMAT_RGBA_8888. */
    public static Bitmap toUprightBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();

        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap padded = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
                Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        Bitmap source = rowPadding == 0 ? padded
                : Bitmap.createBitmap(padded, 0, 0, width, height);
        if (source != padded) padded.recycle();

        int degrees = image.getImageInfo().getRotationDegrees();
        if (degrees == 0) return source;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(source, 0, 0, source.getWidth(),
                source.getHeight(), matrix, true);
        if (rotated != source) source.recycle();
        return rotated;
    }
}
