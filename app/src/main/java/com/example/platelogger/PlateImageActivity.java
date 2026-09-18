package com.example.platelogger;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.IOException;

public final class PlateImageActivity extends AppCompatActivity {
    private static final String EXTRA_IMAGE_PATH = "image_path";
    private static final String EXTRA_PLATE_NUMBER = "plate_number";

    private Bitmap bitmap;

    public static void open(Context context, String imagePath, String plateNumber) {
        Intent intent = new Intent(context, PlateImageActivity.class);
        intent.putExtra(EXTRA_IMAGE_PATH, imagePath);
        intent.putExtra(EXTRA_PLATE_NUMBER, plateNumber);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_image);

        MaterialToolbar toolbar = findViewById(R.id.image_toolbar);
        String plateNumber = getIntent().getStringExtra(EXTRA_PLATE_NUMBER);
        toolbar.setTitle(plateNumber == null || plateNumber.isBlank()
                ? "车牌截图" : plateNumber + " · 完整截图");
        toolbar.setNavigationOnClickListener(view -> finish());

        File imageFile = validatedCapture(getIntent().getStringExtra(EXTRA_IMAGE_PATH));
        if (imageFile == null) {
            Toast.makeText(this, "截图不存在或无法读取", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (bitmap == null) {
            Toast.makeText(this, "截图格式无法解析", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ImageView image = findViewById(R.id.full_plate_image);
        image.setImageBitmap(bitmap);
    }

    private File validatedCapture(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            File captureDirectory = new File(getFilesDir(), "captures").getCanonicalFile();
            File candidate = new File(path).getCanonicalFile();
            String allowedPrefix = captureDirectory.getPath() + File.separator;
            return candidate.getPath().startsWith(allowedPrefix) && candidate.isFile()
                    ? candidate : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        ImageView image = findViewById(R.id.full_plate_image);
        if (image != null) image.setImageDrawable(null);
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        bitmap = null;
        super.onDestroy();
    }
}
