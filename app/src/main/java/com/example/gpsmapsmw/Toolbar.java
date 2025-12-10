package com.example.gpsmapsmw;

import static com.example.gpsmapsmw.MainActivity.ACCESSED_PERMISSIONS;
import static com.example.gpsmapsmw.MainActivity.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
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

import com.google.android.material.appbar.MaterialToolbar;

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

        if (ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[2]) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESSED_PERMISSIONS[3]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(ACCESSED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return;
        };

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
            saveMap();
        } else if (itemId == R.id.share_results) {
            Toast.makeText(this, "Udostępnianie wyników...", Toast.LENGTH_SHORT).show();
            shareInfo();
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
                    sendSMS(userInput);
                });
        builder.show();
    }

    protected abstract void sendSMS(String phoneNumber);
    protected abstract void saveMap();
    protected abstract void shareInfo();
}
