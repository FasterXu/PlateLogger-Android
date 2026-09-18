package com.example.platelogger.data;

public final class PlateRecord {
    public final long id;
    public final String plateNumber;
    public final String plateType;
    public final long capturedAt;
    public final Double latitude;
    public final Double longitude;
    public final String locationProvider;
    public final float confidence;
    public final String imagePath;

    public PlateRecord(long id, String plateNumber, String plateType, long capturedAt, Double latitude,
                       Double longitude, String locationProvider, float confidence,
                       String imagePath) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.plateType = plateType;
        this.capturedAt = capturedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationProvider = locationProvider;
        this.confidence = confidence;
        this.imagePath = imagePath;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
}
