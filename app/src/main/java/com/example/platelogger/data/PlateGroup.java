package com.example.platelogger.data;

public final class PlateGroup {
    public final String plateNumber;
    public final String latestPlateType;
    public final int recordCount;
    public final long lastSeenAt;
    public final String latestImagePath;

    public PlateGroup(String plateNumber, String latestPlateType, int recordCount, long lastSeenAt,
                      String latestImagePath) {
        this.plateNumber = plateNumber;
        this.latestPlateType = latestPlateType;
        this.recordCount = recordCount;
        this.lastSeenAt = lastSeenAt;
        this.latestImagePath = latestImagePath;
    }
}
