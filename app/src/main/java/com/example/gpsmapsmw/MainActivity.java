package com.example.gpsmapsmw;

import static java.lang.String.format;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gpsmapsmw.databinding.ActivityMainBinding;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.overlay.Marker;

import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements LocationListener {
    public static final String TAG = "Mar";
    public static final int PERMISSION_REQUEST_CODE = 1;
    public static final String[] ACCESSED_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.SEND_SMS
    };
    private ActivityMainBinding b;

    private LocationManager locationManager;
    private Marker positionMarker;
    private String bestProvider;
    private int locationUpdateCount = 0;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(b.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(b.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(b.toolbar);

        if (hasNoPermissions()) {
            Toast.makeText(this, "Aplikacja nie będzie działać bez uprawnień!", Toast.LENGTH_SHORT).show();
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        bestProvider = (provider != null) ? provider : LocationManager.GPS_PROVIDER;

        locationManager.requestLocationUpdates(bestProvider, 500, 0.3f, this);

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            updateStatusIcons();
            loadMapView();
        });

        updateStatusIcons();
        loadMapView();
    }

    private void loadMapView() {
        Location location = getLocation();
        if (location == null) {
            Log.v(TAG, "loadMapView: LOCATION IS NULL");
            Toast.makeText(this, "Brak lokalizacji", Toast.LENGTH_SHORT).show();
            return;
        }

        b.archivalDataText.setText(getText(R.string.zapis_lokalizacji));
        updateLocationData(location);

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        Log.v(TAG, format("loadMapView: %s | %s : %s", bestProvider, latitude, longitude));

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        b.mapView.setTileSource(TileSourceFactory.MAPNIK);
        b.mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        b.mapView.setMultiTouchControls(true);

        MapController mapController = (MapController) b.mapView.getController();
        mapController.setZoom(14);

        GeoPoint position = new GeoPoint(latitude, longitude);
        mapController.setCenter(position);
        mapController.animateTo(position);

        updateOrCreateMarker(position);
    }
    private void updateOrCreateMarker(GeoPoint position) {
        if (positionMarker == null) {
            positionMarker = new Marker(b.mapView);
            positionMarker.setTitle("Moja pozycja");
            positionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            positionMarker.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_my_location_24));
            b.mapView.getOverlays().add(positionMarker);
        }
        positionMarker.setPosition(position);
        b.mapView.invalidate();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        GeoPoint newPosition = new GeoPoint(latitude, longitude);
        updateOrCreateMarker(newPosition);

        updateLocationData(location);
        Log.v(TAG, format("Pomiar: %d | %s | %s : %s", ++locationUpdateCount, bestProvider, latitude, longitude));
    }
    private void updateStatusIcons() {
        b.networkIcon.setImageTintList(ColorStateList.valueOf(
                isNetworkAvailable() ? getColor(R.color.green) : getColor(R.color.red)
        ));
        b.gpsIcon.setImageTintList(ColorStateList.valueOf(
                isGPSAvailable() ? getColor(R.color.green) : getColor(R.color.red)
        ));
    }
    private void updateLocationData(Location location) {
        b.bestProviderText.setText(format("%s %s", getText(R.string.najlepszy_dostawca), bestProvider));
        b.longitudeText.setText(format("%s %s", getText(R.string.longitude), location.getLongitude()));
        b.latitudeText.setText(format("%s %s", getText(R.string.latitude), location.getLatitude()));
        b.archivalDataText.append(format("%s : %s\n", location.getLatitude(), location.getLongitude()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.send_sms_menu) {
            showSMSDialog();
            return true;
        } else if (itemId == R.id.save_coords_menu) {
            menuSaveMapImage();
            return true;
        } else if (itemId == R.id.share_results) {
            Toast.makeText(this, "Udostępnianie lokalizacji...", Toast.LENGTH_SHORT).show();
            menuShareLocation();
            return true;
        } else if (itemId == R.id.show_weather) {
            startActivity(new Intent(this, WeatherActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    protected void menuSendLocationSMS(String phoneNumber) {
        Location location = getLocation();
        if (location != null) {
            String text = format("%s : %s", location.getLatitude(), location.getLongitude());
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, text, null, null);

            Toast.makeText(this, "Wysłano SMS", Toast.LENGTH_SHORT).show();
            Log.v(TAG, "sendSMS: SMS sent");
        }
    }
    protected void menuSaveMapImage() {
        b.mapView.setDrawingCacheEnabled(true);
        Bitmap mapBitmap = Bitmap.createBitmap(b.mapView.getDrawingCache());
        b.mapView.setDrawingCacheEnabled(false);

        try {
            String fileName = format("map_%s.png", System.currentTimeMillis());
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
                        Log.v(TAG, "menuSaveMapImage: image saved: " + uri);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Błąd zapisywania zrzutu", Toast.LENGTH_SHORT).show();
            Log.v(TAG, "menuSaveMapImage: saving error", e);
        }
    }
    protected void menuShareLocation() {
        Location location = getLocation();
        if (location == null) {
            Toast.makeText(this, "Brak lokalizacji", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = format("%s : %s", location.getLatitude(), location.getLongitude());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Lokalizacja");

        startActivity(Intent.createChooser(intent, "Udostępnij przez..."));
    }
    private void showSMSDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Wyślij do...")
                .setView(view)
                .setPositiveButton("Wyślij", (dialog, which) -> {
                    EditText input = view.findViewById(R.id.phone_input);
                    String phoneNumber = input.getText().toString();
                    if (phoneNumber.isEmpty()) {
                        Toast.makeText(this, "Musisz podać numer telefonu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    menuSendLocationSMS(phoneNumber);
                });
        builder.show();
    }

    @SuppressLint("MissingPermission")
    private Location getLocation() {
        if (hasNoPermissions())
            return null;
        return locationManager.getLastKnownLocation(bestProvider);
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

    private boolean hasNoPermissions() {
        for (String permission : ACCESSED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(ACCESSED_PERMISSIONS, PERMISSION_REQUEST_CODE);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST_CODE || permissions.length < 1 || grantResults.length < 1) {
            return;
        }

        for(int i = 0; i < permissions.length; i++) {
            Log.v(MainActivity.TAG, format("onRequestPermissionsResult: %d | %s | %d", requestCode, permissions[i], grantResults[i]));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        this.recreate();
    }
}
