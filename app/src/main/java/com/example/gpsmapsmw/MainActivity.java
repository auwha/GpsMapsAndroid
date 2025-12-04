package com.example.gpsmapsmw;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] ACCESSED_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
    };
    private static final String TAG = "Mar";
    private MapView osm;
    private MapController mapController;
    String bestProvider;
    LocationManager locationManager;
    Criteria criteria = new Criteria();
    TextView archivalDataView;
    int amount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[2]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(ACCESSED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        bestProvider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(bestProvider, 500, 0.5f, this);

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.main);
        archivalDataView = findViewById(R.id.archival_data);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            checkGpsAndInternetConnection();
            loadMap();
        });

        checkGpsAndInternetConnection();
        loadMap();
    }

    private void checkGpsAndInternetConnection() {
        TextView textNetwork = findViewById(R.id.text_network);
        TextView textGps = findViewById(R.id.text_gps);

        if (isNetworkAvailable()) {
            textNetwork.setText("internet connected");
            textNetwork.setTextColor(Color.GREEN);
        } else {
            textNetwork.setText("no internet");
            textNetwork.setTextColor(Color.RED);
        }

        if (locationManager.isLocationEnabled() && locationManager.isProviderEnabled(bestProvider)) {
            textGps.setText("gps enabled");
            textGps.setTextColor(Color.GREEN);
        } else {
            textGps.setText("no gps");
            textGps.setTextColor(Color.RED);
        }
    }

    private void addMarkerToMap(GeoPoint center) {
        Marker marker = new Marker(osm);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_my_location_24));
        osm.getOverlays().clear();
        osm.getOverlays().add(marker);
        osm.invalidate();
        marker.setTitle("My position");
    }

    private void loadMap() {
        if (ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[2]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(ACCESSED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return;
        }

        Log.d(TAG, "onCreate: "+bestProvider);
        Location location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {
            updateInfo(location);
            archivalDataView.setText("Measurment reading:\n\n");

            locationManager.requestLocationUpdates(bestProvider, 500, 0.5f, this);

            Log.d(TAG, "onCreate: " + bestProvider + location.getLongitude() + location.getLatitude());


            osm = findViewById(R.id.osm);
            Context context = getApplicationContext();
            Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

            osm.setTileSource(TileSourceFactory.MAPNIK);
            osm.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
            osm.setMultiTouchControls(true);

            mapController = (MapController) osm.getController();
            mapController.setZoom(14);

            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapController.setCenter(geoPoint);
            mapController.animateTo(geoPoint);

            addMarkerToMap(geoPoint);
            osm.addMapListener(new MapListener() {
                @Override
                public boolean onScroll(ScrollEvent event) {
//                    Log.d(TAG, "onScroll: ");
                    return false;
                }

                @Override
                public boolean onZoom(ZoomEvent event) {
//                    Log.d(TAG, "onZoom: ");
                    return false;
                }
            });
        } else {
            Log.d(TAG, "onCreate: LOCATION IS NULL");
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }

        if (permissions[0].equalsIgnoreCase(Manifest.permission.ACCESS_FINE_LOCATION)) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                Toast.makeText(this, "Permission ACCESS_FINE_LOCATION was granted", Toast.LENGTH_SHORT).show();
                this.recreate();
            } else {
                Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                Toast.makeText(this, "Permission ACCESS_FINE_LOCATION was denied", Toast.LENGTH_SHORT).show();
            }

        } else if (permissions[0].equalsIgnoreCase(Manifest.permission.ACCESS_COARSE_LOCATION)) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                Toast.makeText(this, "Permission ACCESS_COARSE_LOCATION was granted", Toast.LENGTH_SHORT).show();
                this.recreate();
            } else {
                Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                Toast.makeText(this, "Permission ACCESS_COARSE_LOCATION was denied", Toast.LENGTH_SHORT).show();
            }

        } else if (permissions[0].equalsIgnoreCase(Manifest.permission.INTERNET)) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                Toast.makeText(this, "Permission INTERNET was granted", Toast.LENGTH_SHORT).show();
                this.recreate();
            } else {
                Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                Toast.makeText(this, "Permission INTERNET was denied", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        bestProvider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {
            updateInfo(location);
            archivalDataView.setText(archivalDataView.getText() + " " + location.getLongitude() + " : " + location.getLatitude() + "\n");
            amount++;
            Log.d(TAG, "onLocationChanged: " + amount + "pomiar:" + bestProvider + location.getLongitude() + location.getLatitude());
        }
    }

    private void updateInfo(Location location) {
        TextView bestProviderView = findViewById(R.id.best_provider);
        TextView longitudeView = findViewById(R.id.longitude);
        TextView latitudeView = findViewById(R.id.latitude);

        bestProviderView.setText("Best provider: " + bestProvider);
        longitudeView.setText("Longitude: " + location.getLongitude());
        latitudeView.setText("Latitude: " + location.getLatitude());
    }
}
