package com.example.gpsmapsmw;

import static java.lang.String.format;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.OutputStream;
import java.util.Objects;

public class MainActivity extends Toolbar implements LocationListener {
    public static final String TAG = "Mar";
    public static final int PERMISSION_REQUEST_CODE = 1;
    public static final String[] ACCESSED_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.SEND_SMS
    };
    private MapView mapView;
    LocationManager locationManager;
    String bestProvider;
    int locationUpdateCount = 0;

    @SuppressLint("MissingPermission")
    private Location getLocation() {
        if (hasNoPermissions())
            return null;
        return locationManager.getLastKnownLocation(bestProvider);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasNoPermissions()) {
            Toast.makeText(this, "Aplikacja nie będzie działać bez uprawnień!", Toast.LENGTH_SHORT).show();
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        bestProvider = Objects.requireNonNullElse(provider, LocationManager.GPS_PROVIDER);

        locationManager.requestLocationUpdates(bestProvider, 500, 0.3f, this);

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            updateStatusIcons();
            loadMap();
        });

        updateStatusIcons();
        loadMap();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (hasNoPermissions())
            return;

        updateLocationData(location);
        Log.v(TAG, format("Pomiar: %d | %s | %s", ++locationUpdateCount, bestProvider, formatLocationData(location)));
    }

    private void updateStatusIcons() {
        ImageView networkIcon = findViewById(R.id.network_icon);
        ImageView gpsIcon = findViewById(R.id.gps_icon);

        int colorNetwork = isNetworkAvailable() ? getColor(R.color.green) : getColor(R.color.red);
        int colorGPS = isGPSAvailable() ? getColor(R.color.green) : getColor(R.color.red);

        networkIcon.setImageTintList(ColorStateList.valueOf(colorNetwork));
        gpsIcon.setImageTintList(ColorStateList.valueOf(colorGPS));
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private boolean isGPSAvailable() {
        return locationManager.isLocationEnabled() && locationManager.isProviderEnabled(bestProvider);
    }

    private void loadMap() {
        Location location = getLocation();
        if (location == null) {
            Log.v(TAG, "loadMap: LOCATION IS NULL");
            return;
        }

        TextView archivalDataText = findViewById(R.id.archival_data);
        archivalDataText.setText(getText(R.string.zapis_lokalizacji));
        updateLocationData(location);

        Log.v(TAG, format("loadMap: %s | %s", bestProvider, formatLocationData(location)));

        mapView = findViewById(R.id.osm);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        mapView.setMultiTouchControls(true);

        MapController mapController = (MapController) mapView.getController();
        mapController.setZoom(14);

        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapController.setCenter(geoPoint);
        mapController.animateTo(geoPoint);
        addMarkerToMap(geoPoint);
    }
    private void addMarkerToMap(GeoPoint center) {
        Marker marker = new Marker(mapView);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_my_location_24));
        mapView.getOverlays().clear();
        mapView.getOverlays().add(marker);
        mapView.invalidate();
        marker.setTitle("My position");
    }

    private String formatLocationData(Location location) {
        if (location == null) {
            Toast.makeText(getApplicationContext(), "Brak lokalizacji", Toast.LENGTH_SHORT).show();
            return "Location is unavailable";
        }

        return format("%s : %s", location.getLongitude(), location.getLatitude());
    }

    private void updateLocationData(Location location) {
        TextView bestProviderText = findViewById(R.id.best_provider);
        TextView longitudeText = findViewById(R.id.longitude);
        TextView latitudeText = findViewById(R.id.latitude);
        TextView archivalDataText = findViewById(R.id.archival_data);

        bestProviderText.setText(format("%s %s", getText(R.string.najlepszy_dostawca), bestProvider));
        longitudeText.setText(format("%s %s", getText(R.string.longitude), location.getLongitude()));
        latitudeText.setText(format("%s %s", getText(R.string.latitude), location.getLatitude()));
        archivalDataText.setText(format("%s %s\n", archivalDataText.getText(), formatLocationData(location)));
    }

    @Override
    protected void sendLocationSMS(String phoneNumber) {
        if (hasNoPermissions())
            return;

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Musisz podać numer telefonu", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "sendSMS: Text empty");
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, formatLocationData(getLocation()), null, null);

        Toast.makeText(this, "Wysłano SMS", Toast.LENGTH_SHORT).show();
        Log.v(TAG, "sendSMS: SMS sent");
    }

    @Override
    protected void saveMapRender() {
        mapView.setDrawingCacheEnabled(true);
        Bitmap mapBitmap = Bitmap.createBitmap(mapView.getDrawingCache());
        mapView.setDrawingCacheEnabled(false);

        try {
            String fileName = "map_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                    if (outputStream != null) {
                        mapBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        Toast.makeText(this, "Zrzut mapy zapisany", Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "saveMapRender: image saved: " + uri);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Błąd zapisywania zrzutu", Toast.LENGTH_SHORT).show();
            Log.v(TAG, "saveMapRender: saving error", e);
        }
    }

    @Override
    protected void shareLocationData() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, formatLocationData(getLocation()));

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Log.v(TAG, "shareLocationData: "+e);
        }
    }
}
