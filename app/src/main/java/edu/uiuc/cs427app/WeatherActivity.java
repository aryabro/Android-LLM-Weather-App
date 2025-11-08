package edu.uiuc.cs427app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import edu.uiuc.cs427app.BuildConfig;
import edu.uiuc.cs427app.City;
import edu.uiuc.cs427app.CityDao;
import edu.uiuc.cs427app.DatabaseClient;

public class WeatherActivity extends BaseActivity implements View.OnClickListener {

    private String cityName;
    private double cityLat;
    private double cityLng;
    private TextView cityTitleView;
    private TextView dateTimeView;
    private TextView temperatureView;
    private TextView conditionView;
    private TextView humidityView;
    private TextView windView;
    private TextView errorView;
    private Button insightsButton;
    private OkHttpClient client;
    private Gson gson;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Try to get cityId first, then fall back to city name
        int cityId = getIntent().getIntExtra("cityId", -1);
        cityName = getIntent().getStringExtra("city");
        cityLat = getIntent().getDoubleExtra("lat", 0.0);
        cityLng = getIntent().getDoubleExtra("lng", 0.0);

        cityTitleView = findViewById(R.id.weatherCityTitle);
        dateTimeView = findViewById(R.id.weatherDateTime);
        temperatureView = findViewById(R.id.weatherTemperature);
        conditionView = findViewById(R.id.weatherCondition);
        humidityView = findViewById(R.id.weatherHumidity);
        windView = findViewById(R.id.weatherWind);
        errorView = findViewById(R.id.weatherError);
        insightsButton = findViewById(R.id.weatherInsightsButton);

        client = new OkHttpClient();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());

        insightsButton.setOnClickListener(this);

        // If cityId is provided, fetch city info from database
        if (cityId != -1) {
            fetchCityFromDatabaseById(cityId);
        } else if (cityLat == 0.0 && cityLng == 0.0) {
            // If coordinates not provided, fetch from database by name
            fetchCoordinatesFromDatabase();
        } else {
            // Fetch weather data with provided coordinates
            if (cityName != null) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Weather - " + cityName);
                }
                cityTitleView.setText(cityName);
                dateTimeView.setText(getFormattedCityDateTime(cityName));
            }
            fetchWeatherData();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.weatherInsightsButton) {
            Intent intent = new Intent(this, WeatherInsightsActivity.class);
            intent.putExtra("city", cityName);
            startActivity(intent);
        }
    }

    private String getFormattedCityDateTime(String city) {
        ZoneId zoneId = resolveZoneIdForCity(city);
        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), zoneId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy • HH:mm z", Locale.getDefault());
        return now.format(fmt);
    }

    private ZoneId resolveZoneIdForCity(String city) {
        if (city == null) {
            return ZoneId.systemDefault();
        }
        String key = city.trim().toLowerCase(Locale.ROOT);
        Map<String, String> known = getKnownCityTimezones();
        if (known.containsKey(key)) {
            try {
                return ZoneId.of(known.get(key));
            } catch (Exception ignored) {
            }
        }
        return ZoneId.systemDefault();
    }

    private Map<String, String> getKnownCityTimezones() {
        Map<String, String> map = new HashMap<>();
        map.put("new york", "America/New_York");
        map.put("chicago", "America/Chicago");
        map.put("los angeles", "America/Los_Angeles");
        map.put("london", "Europe/London");
        map.put("paris", "Europe/Paris");
        map.put("beijing", "Asia/Shanghai");
        map.put("shanghai", "Asia/Shanghai");
        map.put("tokyo", "Asia/Tokyo");
        map.put("sydney", "Australia/Sydney");
        map.put("delhi", "Asia/Kolkata");
        map.put("mumbai", "Asia/Kolkata");
        map.put("singapore", "Asia/Singapore");
        map.put("berlin", "Europe/Berlin");
        map.put("toronto", "America/Toronto");
        map.put("vancouver", "America/Vancouver");
        return map;
    }

    private void fetchCityFromDatabaseById(int cityId) {
        new Thread(() -> {
            try {
                CityDao dao = DatabaseClient.getInstance(this).getAppDatabase().cityDao();
                City city = dao.findById(cityId);
                if (city != null) {
                    cityName = city.getCity();
                    cityLat = city.getLat();
                    cityLng = city.getLng();
                    mainHandler.post(() -> {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Weather - " + cityName);
                        }
                        cityTitleView.setText(cityName);
                        dateTimeView.setText(getFormattedCityDateTime(cityName));
                        if (cityLat != 0.0 || cityLng != 0.0) {
                            fetchWeatherData();
                        } else {
                            showError("Coordinates not found for " + cityName);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        showError("City not found in database with ID: " + cityId);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showError("Failed to fetch city: " + e.getMessage());
                });
            }
        }).start();
    }

    private void fetchCoordinatesFromDatabase() {
        new Thread(() -> {
            try {
                CityDao dao = DatabaseClient.getInstance(this).getAppDatabase().cityDao();
                List<City> cities = dao.findAllByName(cityName);
                if (cities != null && !cities.isEmpty()) {
                    City city = cities.get(0);
                    cityLat = city.getLat();
                    cityLng = city.getLng();
                    mainHandler.post(() -> {
                        if (cityLat != 0.0 || cityLng != 0.0) {
                            fetchWeatherData();
                        } else {
                            showError("Coordinates not found for " + cityName);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        showError("City not found in database: " + cityName);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showError("Failed to fetch coordinates: " + e.getMessage());
                });
            }
        }).start();
    }

    private void fetchWeatherData() {
        // Get API key from local.properties via BuildConfig
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        Log.d("WeatherActivity", "API Key from BuildConfig (raw): " + apiKey);
        Log.d("WeatherActivity", "API Key length: " + (apiKey != null ? apiKey.length() : 0));

        // Remove quotes if present and trim
        if (apiKey != null) {
            apiKey = apiKey.replace("\"", "").trim();
        }

        // Fallback to hardcoded key if not configured (for testing)
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("\"\"") || apiKey.equals("")) {
            Log.w("WeatherActivity", "API key not found in BuildConfig, using fallback");
            apiKey = "994cc37a9f641179032b6ec366feb52d";
        }

        Log.d("WeatherActivity",
                "Using API key: " + (apiKey != null && apiKey.length() > 5 ? apiKey.substring(0, 5) + "..." : apiKey));

        if (cityLat == 0.0 && cityLng == 0.0) {
            showError("Invalid coordinates for " + cityName + " (lat: " + cityLat + ", lng: " + cityLng + ")");
            Log.e("WeatherActivity", "Invalid coordinates: lat=" + cityLat + ", lng=" + cityLng);
            return;
        }

        Log.d("WeatherActivity", "Fetching weather for: " + cityName + " at (" + cityLat + ", " + cityLng + ")");

        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?lat=" +
                    cityLat +
                    "&lon=" + cityLng +
                    "&appid=" + apiKey +
                    "&units=metric";

            Log.d("WeatherActivity", "Request URL: " + url.replace(apiKey, "API_KEY_HIDDEN"));

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("WeatherActivity", "Network request failed", e);
                    mainHandler.post(() -> {
                        showError("Failed to fetch weather data: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d("WeatherActivity", "Response code: " + response.code());
                    Log.d("WeatherActivity",
                            "Response body: " + responseBody.substring(0, Math.min(200, responseBody.length())));

                    if (!response.isSuccessful()) {
                        Log.e("WeatherActivity",
                                "Unsuccessful response: " + response.code() + ", body: " + responseBody);
                        mainHandler.post(() -> {
                            showError("Failed to fetch weather data. Response code: " + response.code() + ". " +
                                    (responseBody.length() > 100 ? responseBody.substring(0, 100) : responseBody));
                        });
                        return;
                    }

                    try {
                        WeatherData weatherData = gson.fromJson(responseBody, WeatherData.class);
                        Log.d("WeatherActivity", "Parsed weather data successfully");
                        mainHandler.post(() -> {
                            updateWeatherUI(weatherData);
                        });
                    } catch (Exception e) {
                        Log.e("WeatherActivity", "Failed to parse weather data", e);
                        Log.e("WeatherActivity", "Response body was: " + responseBody);
                        mainHandler.post(() -> {
                            showError("Failed to parse weather data: " + e.getMessage());
                        });
                    }
                }
            });
        } catch (Exception e) {
            showError("Failed to create request: " + e.getMessage());
        }
    }

    private void updateWeatherUI(WeatherData weatherData) {
        if (weatherData == null) {
            Log.e("WeatherActivity", "WeatherData is null");
            showError("No weather data received");
            return;
        }

        Log.d("WeatherActivity", "Updating UI with weather data");
        errorView.setVisibility(View.GONE);

        // Temperature
        if (weatherData.getMain() != null) {
            double temp = weatherData.getMain().getTemp();
            temperatureView.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
        } else {
            temperatureView.setText("N/A");
        }

        // Weather condition
        if (weatherData.getWeather() != null && weatherData.getWeather().length > 0) {
            String condition = weatherData.getWeather()[0].getDescription();
            // Capitalize first letter
            if (condition != null && !condition.isEmpty()) {
                condition = condition.substring(0, 1).toUpperCase() + condition.substring(1);
            }
            conditionView.setText(condition);
        } else {
            conditionView.setText("N/A");
        }

        // Humidity
        if (weatherData.getMain() != null) {
            int humidity = weatherData.getMain().getHumidity();
            humidityView.setText(String.format(Locale.getDefault(), "%d%%", humidity));
        } else {
            humidityView.setText("N/A");
        }

        // Wind condition
        if (weatherData.getWind() != null) {
            double speed = weatherData.getWind().getSpeed();
            double deg = weatherData.getWind().getDeg();
            String direction = getWindDirection(deg);
            windView.setText(String.format(Locale.getDefault(), "%.1f m/s %s", speed, direction));
        } else {
            windView.setText("N/A");
        }
    }

    private String getWindDirection(double degrees) {
        String[] directions = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW" };
        int index = (int) Math.round(degrees / 22.5) % 16;
        return directions[index];
    }

    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
        temperatureView.setText("Error");
        conditionView.setText("Error");
        humidityView.setText("Error");
        windView.setText("Error");
    }
}
