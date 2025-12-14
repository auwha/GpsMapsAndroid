package com.example.gpsmapsmw;

import static java.lang.String.format;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gpsmapsmw.databinding.ActivityWeatherBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

public class WeatherActivity extends AppCompatActivity {

    private final String CITY = "Torun,pl";
    private final String MY_API = "468aba332aa999335ee94d572d5a56a7";
    private ActivityWeatherBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityWeatherBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(b.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(b.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        new Thread(this::processWeatherData).start();
    }

    private void processWeatherData() {
        String response = HttpRequest.excuteGet(format("https://api.openweathermap.org/data/2.5/weather?lang=pl&q=%s&units=metric&appid=%s", CITY, MY_API));
        if (response == null) {
            return;
        }

        try {
            JSONObject json = new JSONObject(response);

            JSONObject main = json.getJSONObject("main");
            double temperature = main.getDouble("temp");
            double feelsLike = main.getDouble("feels_like");

            JSONArray weatherArray = json.getJSONArray("weather");
            JSONObject weather = weatherArray.getJSONObject(0);
            String iconCode = weather.getString("icon");
            String description = weather.getString("description");

            String iconUrl = format("https://openweathermap.org/img/wn/%s@2x.png", iconCode);
            Bitmap weatherIcon = HttpRequest.downloadImage(iconUrl);

            runOnUiThread(() -> {
                b.temperatureText.setText(format("%s°C", temperature));
                b.feelsLikeText.setText(format("Odczuwalna: \n %s°C", feelsLike));
                b.descriptionText.setText(description);
                if (weatherIcon != null) {
                    b.weatherIcon.setImageBitmap(weatherIcon);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}