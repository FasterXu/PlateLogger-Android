package com.example.platelogger.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class PlateDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "plate_records.db";
    private static final int DB_VERSION = 2;

    public PlateDatabase(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE plate_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "plate_number TEXT NOT NULL," +
                "plate_type TEXT NOT NULL DEFAULT '未分类'," +
                "captured_at INTEGER NOT NULL," +
                "latitude REAL," +
                "longitude REAL," +
                "location_provider TEXT," +
                "confidence REAL NOT NULL," +
                "image_path TEXT NOT NULL)");
        db.execSQL("CREATE INDEX idx_plate_time ON plate_records(plate_number, captured_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE plate_records ADD COLUMN plate_type " +
                    "TEXT NOT NULL DEFAULT '未分类'");
        }
    }

    public synchronized long insert(String plateNumber, String plateType, long capturedAt, Double latitude,
                                    Double longitude, String provider, float confidence,
                                    String imagePath) {
        ContentValues values = new ContentValues();
        values.put("plate_number", plateNumber);
        values.put("plate_type", plateType);
        values.put("captured_at", capturedAt);
        if (latitude == null) values.putNull("latitude"); else values.put("latitude", latitude);
        if (longitude == null) values.putNull("longitude"); else values.put("longitude", longitude);
        values.put("location_provider", provider);
        values.put("confidence", confidence);
        values.put("image_path", imagePath);
        return getWritableDatabase().insertOrThrow("plate_records", null, values);
    }

    public synchronized long latestTimestampFor(String plateNumber) {
        try (Cursor cursor = getReadableDatabase().query(
                "plate_records", new String[]{"MAX(captured_at)"},
                "plate_number = ?", new String[]{plateNumber},
                null, null, null)) {
            return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getLong(0) : 0L;
        }
    }

    public synchronized List<PlateRecord> getAll() {
        return queryRecords(null, null);
    }

    public synchronized List<PlateRecord> getByPlate(String plateNumber) {
        return queryRecords("plate_number = ?", new String[]{plateNumber});
    }

    private List<PlateRecord> queryRecords(String selection, String[] selectionArgs) {
        List<PlateRecord> records = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "plate_records", null, selection, selectionArgs, null, null,
                "captured_at DESC, id DESC")) {
            while (cursor.moveToNext()) {
                Double latitude = cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))
                        ? null : cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                Double longitude = cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))
                        ? null : cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                records.add(new PlateRecord(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("plate_number")),
                        cursor.getString(cursor.getColumnIndexOrThrow("plate_type")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("captured_at")),
                        latitude,
                        longitude,
                        cursor.getString(cursor.getColumnIndexOrThrow("location_provider")),
                        cursor.getFloat(cursor.getColumnIndexOrThrow("confidence")),
                        cursor.getString(cursor.getColumnIndexOrThrow("image_path"))));
            }
        }
        return records;
    }

    public synchronized List<PlateGroup> getPlateGroups() {
        List<PlateGroup> groups = new ArrayList<>();
        String sql = "SELECT p.plate_number, COUNT(*) AS record_count, " +
                "MAX(p.captured_at) AS last_seen_at, " +
                "(SELECT p2.plate_type FROM plate_records p2 " +
                "WHERE p2.plate_number = p.plate_number " +
                "ORDER BY p2.captured_at DESC, p2.id DESC LIMIT 1) AS latest_plate_type, " +
                "(SELECT p2.image_path FROM plate_records p2 " +
                "WHERE p2.plate_number = p.plate_number " +
                "ORDER BY p2.captured_at DESC, p2.id DESC LIMIT 1) AS latest_image_path " +
                "FROM plate_records p GROUP BY p.plate_number " +
                "ORDER BY last_seen_at DESC";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                groups.add(new PlateGroup(
                        cursor.getString(cursor.getColumnIndexOrThrow("plate_number")),
                        cursor.getString(cursor.getColumnIndexOrThrow("latest_plate_type")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("record_count")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("last_seen_at")),
                        cursor.getString(cursor.getColumnIndexOrThrow("latest_image_path"))));
            }
        }
        return groups;
    }

    /** Deletes every observation for one plate and returns its capture paths. */
    public synchronized List<String> deleteByPlate(String plateNumber) {
        return deleteRecords("plate_number = ?", new String[]{plateNumber});
    }

    /** Deletes all observations and returns all capture paths. */
    public synchronized List<String> deleteAllRecords() {
        return deleteRecords(null, null);
    }

    private List<String> deleteRecords(String selection, String[] selectionArgs) {
        SQLiteDatabase db = getWritableDatabase();
        List<String> paths = new ArrayList<>();
        db.beginTransaction();
        try (Cursor cursor = db.query("plate_records", new String[]{"image_path"},
                selection, selectionArgs, null, null, null)) {
            while (cursor.moveToNext()) paths.add(cursor.getString(0));
            db.delete("plate_records", selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return paths;
    }
}
