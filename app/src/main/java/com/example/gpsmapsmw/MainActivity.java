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
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int ACCESS_FINE_LOCATION_PERMISSION = 1;
    private static final int ACCESS_COARSE_LOCATION_PERMISSION = 2;
    private static final String TAG = "Mar";
    private MapView osm;
    private MapController mapController;
    String bestProvider;
    LocationManager locationManager;
    Criteria criteria;
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

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.main);
        TextView textNetwork = findViewById(R.id.text_network);
        TextView textGps = findViewById(R.id.text_gps);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            if (isNetworkAvailable()) {
                textNetwork.setText("internet connected");
                textNetwork.setTextColor(Color.GREEN);
            } else {
                textNetwork.setText("no internet");
                textNetwork.setTextColor(Color.RED);
            }
        });

        criteria = new Criteria();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        bestProvider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_PERMISSION);
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_COARSE_LOCATION_PERMISSION);
            return;
        }

        Location location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {
            updateInfo(location);
            archivalDataView = findViewById(R.id.archival_data);
            archivalDataView.setText("Measurment reading:\n\n");

            locationManager.requestLocationUpdates(bestProvider, 500, 0.5f, this);

            Log.d(TAG, "onCreate: " + bestProvider + location.getLongitude() + location.getLatitude());
        }

        osm = findViewById(R.id.osm);
        Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
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

        switch (requestCode) {
            case ACCESS_FINE_LOCATION_PERMISSION:
                if ((permissions[0].equalsIgnoreCase(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                    Toast.makeText(this, "Permission ACCESS_FINE_LOCATION was granted", Toast.LENGTH_SHORT).show();
                    this.recreate();
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                    Toast.makeText(this, "Permission ACCESS_FINE_LOCATION was denied", Toast.LENGTH_SHORT).show();
                    return;
                }


                break;
            case ACCESS_COARSE_LOCATION_PERMISSION:
                if ((permissions[0].equalsIgnoreCase(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                    Toast.makeText(this, "Permission ACCESS_COARSE_LOCATION was granted", Toast.LENGTH_SHORT).show();
                    this.recreate();
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: "+requestCode+permissions[0]+grantResults[0]);
                    Toast.makeText(this, "Permission ACCESS_COARSE_LOCATION was denied", Toast.LENGTH_SHORT).show();
                    return;
                }

                break;

        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        bestProvider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
