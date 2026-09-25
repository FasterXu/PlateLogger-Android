package com.example.platelogger;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.platelogger.data.PlateDatabase;
import com.example.platelogger.data.PlateNote;
import com.example.platelogger.data.PlateRecord;
import com.example.platelogger.ui.RecordAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PlateDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PLATE_NUMBER = "plate_number";

    private ExecutorService worker;
    private PlateDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_detail);

        String plateNumber = getIntent().getStringExtra(EXTRA_PLATE_NUMBER);
        if (plateNumber == null || plateNumber.isBlank()) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.detail_toolbar);
        toolbar.setTitle(plateNumber);
        toolbar.setNavigationOnClickListener(v -> finish());
        TextView summary = findViewById(R.id.detail_summary);
        EditText noteInput = findViewById(R.id.plate_note_input);
        MaterialButton saveNoteButton = findViewById(R.id.save_note_button);
        RecyclerView list = findViewById(R.id.detail_records_list);
        RecordAdapter adapter = new RecordAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        database = new PlateDatabase(this);
        worker = Executors.newSingleThreadExecutor();
        saveNoteButton.setOnClickListener(view ->
                saveNote(plateNumber, noteInput, saveNoteButton));
        worker.execute(() -> {
            List<PlateRecord> records = database.getByPlate(plateNumber);
            String note = database.getNote(plateNumber);
            runOnUiThread(() -> {
                summary.setText("共 " + records.size() + " 次记录 · 按时间倒序");
                noteInput.setText(note);
                adapter.submit(records);
            });
        });
    }

    private void saveNote(String plateNumber, EditText noteInput, MaterialButton saveButton) {
        String note = PlateNote.normalize(noteInput.getText().toString());
        saveButton.setEnabled(false);
        worker.execute(() -> {
            database.setNote(plateNumber, note);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                noteInput.setText(note);
                noteInput.setSelection(note.length());
                saveButton.setEnabled(true);
                setResult(RESULT_OK);
                Toast.makeText(this, note.isEmpty() ? "备注已清除" : "备注已保存",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (worker != null) worker.shutdown();
        super.onDestroy();
    }
}
