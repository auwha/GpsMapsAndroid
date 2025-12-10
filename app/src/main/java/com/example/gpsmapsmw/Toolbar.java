package com.example.gpsmapsmw;

import static com.example.gpsmapsmw.MainActivity.ACCESSED_PERMISSIONS;
import static com.example.gpsmapsmw.MainActivity.PERMISSION_REQUEST_CODE;

import static java.lang.String.format;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class Toolbar extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (hasNoPermissions())
            return;

        setSupportActionBar(findViewById(R.id.toolbar));
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
            Toast.makeText(this, "Wysyłanie koordynatów...", Toast.LENGTH_SHORT).show();
            showTextInputDialog();
        } else if (itemId == R.id.save_coords_menu) {
            Toast.makeText(this, "Zapisywanie koordynatów...", Toast.LENGTH_SHORT).show();
            saveMapRender();
        } else if (itemId == R.id.share_results) {
            Toast.makeText(this, "Udostępnianie wyników...", Toast.LENGTH_SHORT).show();
            shareLocationData();
        } else if (itemId == R.id.show_weather) {
            Toast.makeText(this, "Wyświetlanie pogody...", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTextInputDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Numer telefonu");

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Wpisz numer telefonu")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String userInput = input.getText().toString();
                    sendLocationSMS(userInput);
                });
        builder.show();
    }

    protected abstract void sendLocationSMS(String phoneNumber);
    protected abstract void saveMapRender();
    protected abstract void shareLocationData();

    protected boolean hasNoPermissions() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESSED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESSED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESSED_PERMISSIONS[2]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESSED_PERMISSIONS[3]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(ACCESSED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST_CODE || permissions.length < 1 || grantResults.length < 1) {
            return;
        }

        Log.v(MainActivity.TAG, format("onRequestPermissionsResult: %d | %s | %d", requestCode, permissions[0], grantResults[0]));
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            this.recreate();
    }
}
