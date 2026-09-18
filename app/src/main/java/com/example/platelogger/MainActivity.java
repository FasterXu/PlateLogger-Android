package com.example.platelogger;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.platelogger.data.PlateDatabase;
import com.example.platelogger.data.PlateGroup;
import com.example.platelogger.data.PlateRecord;
import com.example.platelogger.export.RecordExporter;
import com.example.platelogger.location.LocationTracker;
import com.example.platelogger.recognition.CapturePolicy;
import com.example.platelogger.recognition.CropBounds;
import com.example.platelogger.recognition.ImageConverter;
import com.example.platelogger.recognition.PlateCategory;
import com.example.platelogger.recognition.PlateText;
import com.example.platelogger.ui.PlateBadgeStyler;
import com.example.platelogger.ui.PlateGroupAdapter;
import com.example.platelogger.ui.ScreenAwakePolicy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.common.util.concurrent.ListenableFuture;
import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.HyperLPRParameter;
import com.hyperai.hyperlpr3.bean.Plate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private static final long ANALYSIS_INTERVAL_MS = 500L;
    private static final String RECOGNITION_PREFERENCES = "recognition_settings";
    private static final String CONFIDENCE_THRESHOLD_KEY = "confidence_threshold";
    private static final float DEFAULT_CONFIDENCE_THRESHOLD = 0.88f;
    private static final String KEEP_SCREEN_AWAKE_KEY = "keep_screen_awake";
    private static final String DIM_AFTER_INACTIVITY_KEY = "dim_after_inactivity";
    private static final long DIM_DELAY_MS = 30_000L;
    private static final float DIMMED_SCREEN_BRIGHTNESS = 0.03f;

    private PreviewView previewView;
    private View scannerPage;
    private View historyPage;
    private View scannerFrame;
    private View focusIndicator;
    private View dimOverlay;
    private TextView statusText;
    private TextView currentPlate;
    private TextView plateTypeText;
    private TextView recordCount;
    private TextView emptyText;
    private RecyclerView recordsList;
    private MaterialButton exportButton;
    private MaterialButton deleteAllButton;
    private MaterialButton confidenceButton;
    private MaterialButton displaySettingsButton;

    private PlateDatabase database;
    private LocationTracker locationTracker;
    private PlateGroupAdapter plateGroupAdapter;
    private ExecutorService worker;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private final Runnable hideFocusIndicatorTask = this::hideFocusIndicator;
    private final Handler inactivityHandler = new Handler(Looper.getMainLooper());
    private final Runnable enterDimModeTask = this::enterDimMode;
    private volatile float confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD;
    private volatile boolean engineReady;
    private volatile long lastAnalysisAt;
    private boolean showingScanner = true;
    private boolean activityResumed;
    private boolean keepScreenAwake = true;
    private boolean dimAfterInactivity = true;
    private boolean screenDimmed;
    private float brightnessBeforeDim = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<String> exportLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupPreviewControls();
        scannerPage.addOnLayoutChangeListener((view, left, top, right, bottom,
                                                oldLeft, oldTop, oldRight, oldBottom) ->
                positionScannerFrame());

        SharedPreferences preferences = getSharedPreferences(
                RECOGNITION_PREFERENCES, MODE_PRIVATE);
        confidenceThreshold = Math.max(0.70f, Math.min(0.98f, preferences.getFloat(
                CONFIDENCE_THRESHOLD_KEY, DEFAULT_CONFIDENCE_THRESHOLD)));
        keepScreenAwake = preferences.getBoolean(KEEP_SCREEN_AWAKE_KEY, true);
        dimAfterInactivity = preferences.getBoolean(DIM_AFTER_INACTIVITY_KEY, true);
        updateConfidenceButton();
        updateDisplaySettingsButton();

        database = new PlateDatabase(this);
        locationTracker = new LocationTracker(this);
        plateGroupAdapter = new PlateGroupAdapter(plateNumber -> {
            Intent intent = new Intent(this, PlateDetailActivity.class);
            intent.putExtra(PlateDetailActivity.EXTRA_PLATE_NUMBER, plateNumber);
            startActivity(intent);
        });
        worker = Executors.newSingleThreadExecutor();

        recordsList.setLayoutManager(new LinearLayoutManager(this));
        recordsList.setAdapter(plateGroupAdapter);
        setupSwipeToDelete();
        setupActivityResultLaunchers();
        setupNavigation();
        exportButton.setOnClickListener(v -> beginExport());
        deleteAllButton.setOnClickListener(v -> confirmDeleteAll());
        confidenceButton.setOnClickListener(v -> showConfidenceDialog());
        displaySettingsButton.setOnClickListener(v -> showDisplaySettingsDialog());

        initializeRecognitionEngine();
        requestNeededPermissions();
    }

    private void bindViews() {
        previewView = findViewById(R.id.preview);
        scannerPage = findViewById(R.id.scanner_page);
        historyPage = findViewById(R.id.history_page);
        scannerFrame = findViewById(R.id.scanner_frame);
        focusIndicator = findViewById(R.id.focus_indicator);
        dimOverlay = findViewById(R.id.dim_overlay);
        statusText = findViewById(R.id.status_text);
        currentPlate = findViewById(R.id.current_plate);
        plateTypeText = findViewById(R.id.current_plate_type);
        recordCount = findViewById(R.id.record_count);
        emptyText = findViewById(R.id.empty_text);
        recordsList = findViewById(R.id.records_list);
        exportButton = findViewById(R.id.export_button);
        deleteAllButton = findViewById(R.id.delete_all_button);
        confidenceButton = findViewById(R.id.confidence_button);
        displaySettingsButton = findViewById(R.id.display_settings_button);
    }

    private void setupPreviewControls() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(android.view.MotionEvent event) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent event) {
                previewView.performClick();
                focusAt(event.getX(), event.getY());
                return true;
            }
        });
        scaleGestureDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        Camera activeCamera = camera;
                        if (activeCamera == null) return false;
                        ZoomState state = activeCamera.getCameraInfo().getZoomState().getValue();
                        if (state == null) return false;
                        float ratio = Math.max(state.getMinZoomRatio(),
                                Math.min(state.getMaxZoomRatio(),
                                        state.getZoomRatio() * detector.getScaleFactor()));
                        activeCamera.getCameraControl().setZoomRatio(ratio);
                        statusText.setText(String.format(Locale.CHINA,
                                "双指变焦 %.1fx · 点按车牌可对焦", ratio));
                        return true;
                    }
                });
        previewView.setOnTouchListener((view, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void focusAt(float x, float y) {
        Camera activeCamera = camera;
        if (activeCamera == null) return;
        showFocusIndicator(x, y);
        MeteringPoint point = previewView.getMeteringPointFactory().createPoint(x, y);
        FocusMeteringAction action = new FocusMeteringAction.Builder(point,
                FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE
                        | FocusMeteringAction.FLAG_AWB)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build();
        ListenableFuture<FocusMeteringResult> result =
                activeCamera.getCameraControl().startFocusAndMetering(action);
        statusText.setText("正在对焦…");
        result.addListener(() -> {
            try {
                statusText.setText(result.get().isFocusSuccessful()
                        ? "对焦完成 · 正在识别" : "未锁定焦点，请再次点按车牌");
            } catch (Exception error) {
                statusText.setText("对焦失败，请再次点按车牌");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void showFocusIndicator(float x, float y) {
        float halfSize = 28f * getResources().getDisplayMetrics().density;
        focusIndicator.setX(x - halfSize);
        focusIndicator.setY(y - halfSize);
        focusIndicator.setVisibility(View.VISIBLE);
        focusIndicator.removeCallbacks(hideFocusIndicatorTask);
        focusIndicator.postDelayed(hideFocusIndicatorTask, 900L);
    }

    private void hideFocusIndicator() {
        focusIndicator.setVisibility(View.GONE);
    }

    private void positionScannerFrame() {
        if (scannerPage.getHeight() == 0 || scannerFrame.getHeight() == 0) return;
        float centerY = scannerPage.getHeight() * 0.25f;
        scannerFrame.setY(Math.max(0f, centerY - scannerFrame.getHeight() / 2f));
    }

    private void showConfidenceDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24f * getResources().getDisplayMetrics().density);
        content.setPadding(padding, 0, padding, 0);

        TextView valueText = new TextView(this);
        valueText.setTextSize(16f);
        valueText.setText(formatConfidenceValue(confidenceThreshold));
        content.addView(valueText);

        Slider slider = new Slider(this);
        slider.setValueFrom(0.70f);
        slider.setValueTo(0.98f);
        slider.setStepSize(0.01f);
        slider.setValue(confidenceThreshold);
        slider.addOnChangeListener((ignored, value, fromUser) ->
                valueText.setText(formatConfidenceValue(value)));
        content.addView(slider);

        new MaterialAlertDialogBuilder(this)
                .setTitle("调整置信度阈值")
                .setMessage("阈值越高，误识别越少，但弱光或远距离车牌可能更难被识别。")
                .setView(content)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    confidenceThreshold = slider.getValue();
                    getSharedPreferences(RECOGNITION_PREFERENCES, MODE_PRIVATE)
                            .edit()
                            .putFloat(CONFIDENCE_THRESHOLD_KEY, confidenceThreshold)
                            .apply();
                    updateConfidenceButton();
                    statusText.setText("置信度阈值已更新，将按新阈值进行单帧识别");
                })
                .show();
    }

    private void updateConfidenceButton() {
        confidenceButton.setText(String.format(Locale.CHINA, "置信度阈值 %.0f%%",
                confidenceThreshold * 100f));
    }

    private String formatConfidenceValue(float confidence) {
        return String.format(Locale.CHINA, "当前阈值：%.0f%%", confidence * 100f);
    }

    private void showDisplaySettingsDialog() {
        inactivityHandler.removeCallbacks(enterDimModeTask);
        exitDimMode();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = Math.round(24f * getResources().getDisplayMetrics().density);
        content.setPadding(horizontalPadding, 0, horizontalPadding, 0);

        SwitchMaterial keepAwakeSwitch = new SwitchMaterial(this);
        keepAwakeSwitch.setText("扫描时保持屏幕常亮");
        keepAwakeSwitch.setChecked(keepScreenAwake);
        content.addView(keepAwakeSwitch);

        SwitchMaterial dimSwitch = new SwitchMaterial(this);
        dimSwitch.setText("30 秒无操作后进入低亮度模式");
        dimSwitch.setChecked(dimAfterInactivity);
        dimSwitch.setEnabled(keepScreenAwake);
        content.addView(dimSwitch);

        TextView explanation = new TextView(this);
        explanation.setText("低亮度模式不会停止摄像头或识别，轻触屏幕即可恢复亮度。关闭常亮后，手机将按系统设置自动熄屏。");
        explanation.setTextSize(14f);
        explanation.setPadding(0, Math.round(8f * getResources().getDisplayMetrics().density),
                0, 0);
        content.addView(explanation);

        keepAwakeSwitch.setOnCheckedChangeListener((button, checked) -> {
            dimSwitch.setEnabled(checked);
            if (!checked) dimSwitch.setChecked(false);
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("屏幕与省电")
                .setView(content)
                .setNegativeButton("取消", (dialog, which) -> resetDimTimer())
                .setPositiveButton("保存", (dialog, which) -> {
                    keepScreenAwake = keepAwakeSwitch.isChecked();
                    dimAfterInactivity = keepScreenAwake && dimSwitch.isChecked();
                    getSharedPreferences(RECOGNITION_PREFERENCES, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEEP_SCREEN_AWAKE_KEY, keepScreenAwake)
                            .putBoolean(DIM_AFTER_INACTIVITY_KEY, dimAfterInactivity)
                            .apply();
                    updateDisplaySettingsButton();
                    applyScreenPolicy();
                })
                .setOnCancelListener(dialog -> resetDimTimer())
                .show();
    }

    private void updateDisplaySettingsButton() {
        if (!keepScreenAwake) {
            displaySettingsButton.setText("屏幕：跟随系统");
        } else if (dimAfterInactivity) {
            displaySettingsButton.setText("屏幕：常亮 · 30 秒低亮");
        } else {
            displaySettingsButton.setText("屏幕：保持常亮");
        }
    }

    private void applyScreenPolicy() {
        boolean shouldKeepAwake = ScreenAwakePolicy.shouldKeepAwake(
                activityResumed, showingScanner, keepScreenAwake);
        if (shouldKeepAwake) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (ScreenAwakePolicy.shouldScheduleDim(activityResumed, showingScanner,
                keepScreenAwake, dimAfterInactivity)) {
            resetDimTimer();
        } else {
            inactivityHandler.removeCallbacks(enterDimModeTask);
            exitDimMode();
        }
    }

    private void resetDimTimer() {
        inactivityHandler.removeCallbacks(enterDimModeTask);
        exitDimMode();
        if (ScreenAwakePolicy.shouldScheduleDim(activityResumed, showingScanner,
                keepScreenAwake, dimAfterInactivity)) {
            inactivityHandler.postDelayed(enterDimModeTask, DIM_DELAY_MS);
        }
    }

    private void enterDimMode() {
        if (!ScreenAwakePolicy.shouldScheduleDim(activityResumed, showingScanner,
                keepScreenAwake, dimAfterInactivity) || screenDimmed) return;
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        brightnessBeforeDim = attributes.screenBrightness;
        attributes.screenBrightness = DIMMED_SCREEN_BRIGHTNESS;
        getWindow().setAttributes(attributes);
        dimOverlay.setVisibility(View.VISIBLE);
        screenDimmed = true;
    }

    private void exitDimMode() {
        if (!screenDimmed) return;
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.screenBrightness = brightnessBeforeDim;
        getWindow().setAttributes(attributes);
        brightnessBeforeDim = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        dimOverlay.setVisibility(View.GONE);
        screenDimmed = false;
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetDimTimer();
    }

    private void setupSwipeToDelete() {
        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.rgb(179, 38, 30));
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(16f * getResources().getDisplayMetrics().scaledDensity);

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                PlateGroup group = plateGroupAdapter.getItem(position);
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("删除 " + group.plateNumber + "？")
                        .setMessage("该车牌的全部记录和截图都将被删除，此操作无法撤销。")
                        .setNegativeButton("取消", (dialog, which) ->
                                plateGroupAdapter.notifyItemChanged(position))
                        .setPositiveButton("删除", (dialog, which) ->
                                deletePlate(group.plateNumber))
                        .setOnCancelListener(dialog ->
                                plateGroupAdapter.notifyItemChanged(position))
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX,
                                    float dY, int actionState, boolean isCurrentlyActive) {
                if (dX < 0f) {
                    View item = viewHolder.itemView;
                    canvas.drawRect(item.getRight() + dX, item.getTop(), item.getRight(),
                            item.getBottom(), backgroundPaint);
                    float centerX = item.getRight() + dX / 2f;
                    float centerY = item.getTop() + item.getHeight() / 2f
                            - (textPaint.ascent() + textPaint.descent()) / 2f;
                    canvas.drawText("删除", centerX, centerY, textPaint);
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY,
                        actionState, isCurrentlyActive);
            }
        });
        helper.attachToRecyclerView(recordsList);
    }

    private void setupActivityResultLaunchers() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionsResult);
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
                    if (uri != null) exportTo(uri);
                });
    }

    private void setupNavigation() {
        BottomNavigationView navigation = findViewById(R.id.bottom_nav);
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_history) {
                showingScanner = false;
                scannerPage.setVisibility(View.GONE);
                historyPage.setVisibility(View.VISIBLE);
                if (cameraProvider != null) cameraProvider.unbindAll();
                camera = null;
                applyScreenPolicy();
                loadHistory();
                return true;
            }
            showingScanner = true;
            historyPage.setVisibility(View.GONE);
            scannerPage.setVisibility(View.VISIBLE);
            applyScreenPolicy();
            startCameraIfReady();
            return true;
        });
    }

    private void initializeRecognitionEngine() {
        worker.execute(() -> {
            try {
                HyperLPRParameter parameter = new HyperLPRParameter()
                        .setDetLevel(HyperLPR3.DETECT_LEVEL_LOW)
                        .setMaxNum(3)
                        .setThreads(2)
                        .setRecConfidenceThreshold(0.60f);
                HyperLPR3.getInstance().init(getApplicationContext(), parameter);
                engineReady = true;
                runOnUiThread(() -> {
                    statusText.setText(locationTracker.hasPermission()
                            ? "识别已就绪 · 定位已开启" : "识别已就绪 · 当前不记录位置");
                    startCameraIfReady();
                });
            } catch (Throwable error) {
                engineReady = false;
                runOnUiThread(() -> statusText.setText("识别引擎加载失败：" + safeMessage(error)));
            }
        });
    }

    private void requestNeededPermissions() {
        boolean cameraMissing = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED;
        boolean locationMissing = !locationTracker.hasPermission();
        if (cameraMissing || locationMissing) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            locationTracker.start();
            startCameraIfReady();
        }
    }

    private void onPermissionsResult(Map<String, Boolean> result) {
        if (Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION))
                || locationTracker.hasPermission()) {
            locationTracker.start();
        }
        if (hasCameraPermission()) {
            startCameraIfReady();
        } else {
            statusText.setText("相机权限未开启，无法实时识别");
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCameraIfReady() {
        if (!showingScanner || !engineReady || !hasCameraPermission() || isFinishing()) return;
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Exception error) {
                statusText.setText("相机启动失败：" + safeMessage(error));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null || !showingScanner) return;
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();
        analysis.setAnalyzer(worker, this::analyzeFrame);

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            statusText.setText(locationTracker.hasPermission()
                    ? "点按对焦 · 双指变焦 · 正在记录位置"
                    : "点按对焦 · 双指变焦 · 未记录位置");
        } catch (RuntimeException error) {
            statusText.setText("无法绑定后置摄像头：" + safeMessage(error));
        }
    }

    private void analyzeFrame(@NonNull ImageProxy image) {
        Bitmap frame = null;
        try {
            long now = System.currentTimeMillis();
            if (!engineReady || now - lastAnalysisAt < ANALYSIS_INTERVAL_MS) return;
            lastAnalysisAt = now;
            frame = ImageConverter.toUprightBitmap(image);
            Plate[] plates = HyperLPR3.getInstance().plateRecognition(
                    frame, HyperLPR3.CAMERA_ROTATION_0, HyperLPR3.STREAM_BGRA);
            Plate best = bestPlate(plates);
            if (best != null) processRecognition(frame, best, now);
        } catch (Throwable error) {
            runOnUiThread(() -> statusText.setText("本帧识别失败：" + safeMessage(error)));
        } finally {
            if (frame != null && !frame.isRecycled()) frame.recycle();
            image.close();
        }
    }

    private Plate bestPlate(Plate[] plates) {
        Plate best = null;
        if (plates == null) return null;
        for (Plate plate : plates) {
            if (plate.getConfidence() < confidenceThreshold) continue;
            String normalized = PlateText.normalize(plate.getCode());
            if (!PlateText.isLikelyChinesePlate(normalized)) continue;
            if (best == null || plate.getConfidence() > best.getConfidence()) best = plate;
        }
        return best;
    }

    private void processRecognition(Bitmap frame, Plate plate, long capturedAt) throws IOException {
        String code = PlateText.normalize(plate.getCode());
        String plateType = PlateCategory.labelFor(code, plate.getType());
        runOnUiThread(() -> {
            currentPlate.setText(code);
            PlateBadgeStyler.apply(currentPlate, plateType);
            plateTypeText.setText(plateType);
            statusText.setText(String.format(Locale.CHINA,
                    "单帧识别 · 置信度 %.1f%%", plate.getConfidence() * 100f));
        });

        if (!CapturePolicy.shouldRecord(database.latestTimestampFor(code), capturedAt)) {
            runOnUiThread(() -> statusText.setText(
                    "已识别 · 同一车牌 1 分钟内不重复记录"));
            return;
        }

        Bitmap capture = cropPlate(frame, plate);
        String imagePath;
        try {
            imagePath = saveCapture(capture, code, capturedAt);
        } finally {
            capture.recycle();
        }

        Location location = locationTracker.snapshot();
        Double latitude = location == null ? null : location.getLatitude();
        Double longitude = location == null ? null : location.getLongitude();
        String provider = location == null ? null : location.getProvider();
        database.insert(code, plateType, capturedAt, latitude, longitude, provider,
                plate.getConfidence(), imagePath);
        runOnUiThread(() -> {
            statusText.setText("已记录 · " + plateType);
            Toast.makeText(this, "已记录 " + code, Toast.LENGTH_SHORT).show();
        });
    }

    private Bitmap cropPlate(Bitmap source, Plate plate) {
        CropBounds.Bounds bounds = CropBounds.doubled(
                plate.getX1(), plate.getY1(), plate.getX2(), plate.getY2(),
                source.getWidth(), source.getHeight());
        return Bitmap.createBitmap(source, bounds.left, bounds.top,
                bounds.width(), bounds.height());
    }

    private String saveCapture(Bitmap bitmap, String plate, long capturedAt) throws IOException {
        File directory = new File(getFilesDir(), "captures");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("无法创建截图目录");
        File destination = new File(directory,
                capturedAt + "_" + plate.replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5]", "_") + ".jpg");
        try (FileOutputStream output = new FileOutputStream(destination)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
                throw new IOException("截图压缩失败");
            }
        }
        return destination.getAbsolutePath();
    }

    private void loadHistory() {
        worker.execute(() -> {
            List<PlateGroup> groups = database.getPlateGroups();
            int total = 0;
            for (PlateGroup group : groups) total += group.recordCount;
            int totalRecords = total;
            runOnUiThread(() -> {
                plateGroupAdapter.submit(groups);
                recordCount.setText(groups.size() + " 个车牌 · " + totalRecords + " 条记录");
                boolean empty = groups.isEmpty();
                emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
                recordsList.setVisibility(empty ? View.GONE : View.VISIBLE);
                exportButton.setEnabled(!empty);
                deleteAllButton.setEnabled(!empty);
            });
        });
    }

    private void deletePlate(String plateNumber) {
        worker.execute(() -> {
            List<String> imagePaths = database.deleteByPlate(plateNumber);
            deleteCaptureFiles(imagePaths);
            runOnUiThread(() -> {
                Toast.makeText(this, "已删除 " + plateNumber, Toast.LENGTH_SHORT).show();
                loadHistory();
            });
        });
    }

    private void confirmDeleteAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除全部记录？")
                .setMessage("所有车牌记录和截图都将被永久删除，此操作无法撤销。")
                .setNegativeButton("取消", null)
                .setPositiveButton("全部删除", (dialog, which) -> deleteAllRecords())
                .show();
    }

    private void deleteAllRecords() {
        deleteAllButton.setEnabled(false);
        worker.execute(() -> {
            List<String> imagePaths = database.deleteAllRecords();
            deleteCaptureFiles(imagePaths);
            runOnUiThread(() -> {
                Toast.makeText(this, "已删除全部记录", Toast.LENGTH_SHORT).show();
                loadHistory();
            });
        });
    }

    private void deleteCaptureFiles(List<String> paths) {
        try {
            File captureDirectory = new File(getFilesDir(), "captures").getCanonicalFile();
            String allowedPrefix = captureDirectory.getPath() + File.separator;
            for (String path : paths) {
                if (path == null || path.isBlank()) continue;
                File capture = new File(path).getCanonicalFile();
                if (capture.getPath().startsWith(allowedPrefix) && capture.isFile()) {
                    // A failed file deletion does not restore the already deleted database row.
                    capture.delete();
                }
            }
        } catch (IOException ignored) { }
    }

    private void beginExport() {
        worker.execute(() -> {
            List<PlateRecord> records = database.getAll();
            if (records.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "暂无可导出的记录", Toast.LENGTH_SHORT).show());
                return;
            }
            String name = "车牌记录_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
                    .format(new Date()) + ".zip";
            runOnUiThread(() -> exportLauncher.launch(name));
        });
    }

    private void exportTo(Uri uri) {
        exportButton.setEnabled(false);
        worker.execute(() -> {
            try {
                RecordExporter.exportZip(getContentResolver(), uri, database.getAll());
                runOnUiThread(() -> Toast.makeText(this,
                        "导出完成（CSV 与车牌截图）", Toast.LENGTH_LONG).show());
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this,
                        "导出失败：" + safeMessage(error), Toast.LENGTH_LONG).show());
            } finally {
                runOnUiThread(() -> exportButton.setEnabled(true));
            }
        });
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        applyScreenPolicy();
        if (locationTracker != null && locationTracker.hasPermission()) locationTracker.start();
    }

    @Override
    protected void onPause() {
        activityResumed = false;
        applyScreenPolicy();
        if (locationTracker != null) locationTracker.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityHandler.removeCallbacks(enterDimModeTask);
        exitDimMode();
        if (cameraProvider != null) cameraProvider.unbindAll();
        camera = null;
        if (worker != null) worker.shutdown();
        super.onDestroy();
    }
}
