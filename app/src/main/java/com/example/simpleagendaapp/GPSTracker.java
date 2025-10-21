package com.example.simpleagendaapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GPSTracker implements LocationListener {
    private final Context context;
    private final LocationManager locationManager;

    private Location location;
    private double latitude;
    private double longitude;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // μέτρα
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 λεπτό
    private boolean canGetLocation = false;

    public GPSTracker(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    // Λήψη τοποθεσίας από GPS ή δίκτυο
    public Location getLocation() {
        try {
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // Δεν υπάρχει διαθέσιμος πάροχος τοποθεσίας
            } else {
                this.canGetLocation = true;

                // Αν είναι ενεργό το δίκτυο, χρησιμοποίησέ το
                if (isNetworkEnabled) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        try {
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        } catch (SecurityException se) {
                            se.printStackTrace();
                        }
                    }
                }

                // Αν είναι ενεργό το GPS και δεν έχει βρεθεί τοποθεσία από το δίκτυο
                if (isGPSEnabled) {
                    if (location == null) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                            try {
                                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (location != null) {
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                }
                            } catch (SecurityException se) {
                                se.printStackTrace();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    // Χρήση της τοποθεσίας και ανάκτηση πόλης
    public void requestLocationUpdates() {
        getLocation();
        if (location != null) {
            getCityFromCoordinates(location.getLatitude(), location.getLongitude());
        }
    }

    // Ανάκτηση ονόματος πόλης από συντεταγμένες και αποθήκευση
    private void getCityFromCoordinates(double lat, double lon) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                String dateKey = ((MainActivity) context).getSelectedDate();
                SharedPreferences.Editor editor = context.getSharedPreferences("Notes", Context.MODE_PRIVATE).edit();
                editor.putString("location_" + dateKey, city);
                editor.putString("latlng_" + dateKey, lat + "," + lon);
                editor.apply();

                ((MainActivity) context).showMapButton(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Όταν αλλάζει η τοποθεσία, ενημερώνει τις συντεταγμένες και αποθηκεύει νέα πόλη
    @Override
    public void onLocationChanged(@NonNull Location location) {
        this.location = location;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        getCityFromCoordinates(latitude, longitude);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

}
