package com.example.platelogger.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

public final class LocationTracker implements LocationListener {
    private final Context context;
    private final LocationManager manager;
    private volatile Location lastLocation;

    public LocationTracker(Context context) {
        this.context = context.getApplicationContext();
        this.manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        if (!hasPermission()) return;
        updateFromLastKnown(LocationManager.GPS_PROVIDER);
        updateFromLastKnown(LocationManager.NETWORK_PROVIDER);
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 3f, this);
        } catch (RuntimeException ignored) { }
        try {
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 3f, this);
        } catch (RuntimeException ignored) { }
    }

    @SuppressLint("MissingPermission")
    private void updateFromLastKnown(String provider) {
        try {
            Location candidate = manager.getLastKnownLocation(provider);
            if (candidate != null && (lastLocation == null || candidate.getTime() > lastLocation.getTime())) {
                lastLocation = candidate;
            }
        } catch (RuntimeException ignored) { }
    }

    public void stop() {
        try {
            manager.removeUpdates(this);
        } catch (RuntimeException ignored) { }
    }

    public Location snapshot() {
        Location value = lastLocation;
        if (value == null || System.currentTimeMillis() - value.getTime() > 5 * 60_000L) return null;
        return new Location(value);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (lastLocation == null || location.getTime() >= lastLocation.getTime()) {
            lastLocation = location;
        }
    }

    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
}
