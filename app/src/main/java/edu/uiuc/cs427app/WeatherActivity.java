package edu.uiuc.cs427app;

import android.widget.ImageView;
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
import java.time.Clock;

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
import okhttp3.ResponseBody;

public class WeatherActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "WeatherActivity";
    private static final String NOT_AVAILABLE = "N/A";
    private static final String ERROR_TEXT = "Error";

    private String cityName;
    private double cityLat;
    private double cityLng;
    private TextView cityTitleView;
    private TextView dateTimeView;
    private TextView temperatureView;
    private TextView conditionView;
    private TextView humidityView;
    private TextView windView;
    private ImageView weatherImageView;
    private TextView errorView;
    private Button insightsButton;
    private OkHttpClient client;
    private Gson gson;
    private Handler mainHandler;
    private WeatherData currentWeatherData;

    private Clock clock = Clock.systemDefaultZone();

    public void setClock(Clock testClock) {
        this.clock = testClock;
    }

    // creates weather activity
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
        weatherImageView = findViewById(R.id.weatherImage);
        errorView = findViewById(R.id.weatherError);
        insightsButton = findViewById(R.id.weatherInsightsButton);

        client = new OkHttpClient();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
        WeatherAwareView weatherAwareView = new WeatherAwareView(this, weatherImageView);
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

    // sets up weather insights
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.weatherInsightsButton) {
            if (currentWeatherData != null) {
                Intent intent = new Intent(this, WeatherInsightsActivity.class);
                intent.putExtra("city", cityName);

                // dynamic theme support for weatherinsights
                String username = getIntent().getStringExtra("username");
                if (username != null) {
                    intent.putExtra("username", username);
                }

                // Pass weather data
                if (currentWeatherData.getMain() != null) {
                    intent.putExtra("temperature", currentWeatherData.getMain().getTemp());
                    intent.putExtra("humidity", currentWeatherData.getMain().getHumidity());
                }
                if (currentWeatherData.getWeather() != null && currentWeatherData.getWeather().length > 0) {
                    intent.putExtra("condition", currentWeatherData.getWeather()[0].getDescription());
                    intent.putExtra("weatherMain", currentWeatherData.getWeather()[0].getMain());
                }
                if (currentWeatherData.getWind() != null) {
                    intent.putExtra("windSpeed", currentWeatherData.getWind().getSpeed());
                    intent.putExtra("windDeg", currentWeatherData.getWind().getDeg());
                }

                startActivity(intent);
            }
        }
    }

    // for a given city, returns formatted time
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
        String timezone = getKnownCityTimezones().get(key);
    
        if (timezone != null) {
            return ZoneId.of(timezone);
        }
    
        return ZoneId.systemDefault();
    }
    
    // gets city timezones
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

    // gets city name from database (ID)
    private void fetchCityFromDatabaseById(int cityId) {
        AppIdlingResource.increment(); // Mark async operation start
        new Thread(() -> {
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
                        AppIdlingResource.decrement(); // Database query complete
                        if (cityLat != 0.0 || cityLng != 0.0) {
                            fetchWeatherData(); // fetchWeatherData will increment its own counter
                        } else {
                            showError("Coordinates not found for " + cityName);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        showError("City not found in database with ID: " + cityId);
                        AppIdlingResource.decrement(); // Mark async operation complete
                    });
                }
        }).start();
    }

    // gets coordinates
    private void fetchCoordinatesFromDatabase() {
        AppIdlingResource.increment(); // Mark async operation start
        new Thread(() -> {
                CityDao dao = DatabaseClient.getInstance(this).getAppDatabase().cityDao();
                List<City> cities = dao.findAllByName(cityName);
                if (cities != null && !cities.isEmpty()) {
                    City city = cities.get(0);
                    cityLat = city.getLat();
                    cityLng = city.getLng();
                    mainHandler.post(() -> {
                        AppIdlingResource.decrement(); // Database query complete
                        if (cityLat != 0.0 || cityLng != 0.0) {
                            fetchWeatherData(); // fetchWeatherData will increment its own counter
                        } else {
                            showError("Coordinates not found for " + cityName);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        showError("City not found in database: " + cityName);
                        AppIdlingResource.decrement(); // Mark async operation complete
                    });
                }
        }).start();
    }

    // gets weather data from api
    private void fetchWeatherData() {
        // Get API key from local.properties via BuildConfig
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;

        // Remove quotes if present and trim
        if (apiKey != null) {
            apiKey = apiKey.replace("\"", "").trim();
        }

        // Fallback to hardcoded key if not configured (for testing)
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("\"\"") || apiKey.equals("")) {
            Log.w(TAG, "API key not found in BuildConfig, using fallback");
            apiKey = "994cc37a9f641179032b6ec366feb52d";
        }

        if (cityLat == 0.0 && cityLng == 0.0) {
            showError("Invalid coordinates for " + cityName + " (lat: " + cityLat + ", lng: " + cityLng + ")");
            Log.e(TAG, "Invalid coordinates: lat=" + cityLat + ", lng=" + cityLng);
            return;
        }

        Log.d(TAG, "Fetching weather for: " + cityName + " at (" + cityLat + ", " + cityLng + ")");

        AppIdlingResource.increment(); // Mark network request start
        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?lat=" +
                    cityLat +
                    "&lon=" + cityLng +
                    "&appid=" + apiKey +
                    "&units=metric";

            Log.d(TAG, "Request URL: " + url.replace(apiKey, "API_KEY_HIDDEN"));

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network request failed", e);
                    mainHandler.post(() -> {
                        showError("Failed to fetch weather data: " + e.getMessage());
                        AppIdlingResource.decrement(); // Mark network request complete
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    ResponseBody body = response.body();
                    if (body == null) {
                        mainHandler.post(() -> {
                            showError("Empty response from server");
                            AppIdlingResource.decrement(); // Mark network request complete
                        });
                        return;
                    }

                    String responseBody = body.string();
                    Log.d(TAG, "Response code: " + response.code());
                    Log.d(TAG, "Response body: " + responseBody.substring(0, Math.min(200, responseBody.length())));

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Unsuccessful response: " + response.code() + ", body: " + responseBody);
                        String errorMsg = "Failed to fetch weather data. Response code: " + response.code() + ". " +
                                (responseBody.length() > 100 ? responseBody.substring(0, 100) : responseBody);
                        mainHandler.post(() -> {
                            showError(errorMsg);
                            AppIdlingResource.decrement(); // Mark network request complete
                        });
                        return;
                    }

                    try {
                        WeatherData weatherData = gson.fromJson(responseBody, WeatherData.class);
                        Log.d(TAG, "Parsed weather data successfully");
                        mainHandler.post(() -> {
                            updateWeatherUI(weatherData);
                            AppIdlingResource.decrement(); // Mark network request complete after UI update
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse weather data", e);
                        Log.e(TAG, "Response body was: " + responseBody);
                        mainHandler.post(() -> {
                            showError("Failed to parse weather data: " + e.getMessage());
                            AppIdlingResource.decrement(); // Mark network request complete
                        });
                    }
                }
            });
        } catch (Exception e) {
            showError("Failed to create request: " + e.getMessage());
            AppIdlingResource.decrement(); // Mark network request complete on error
        }
    }

    // updates weather UI
    private void updateWeatherUI(WeatherData weatherData) {
        if (weatherData == null) {
            Log.e(TAG, "WeatherData is null");
            showError("No weather data received");
            return;
        }

        // Store weather data for insights
        this.currentWeatherData = weatherData;

        // Enable insights button now that we have data
        insightsButton.setEnabled(true);

        Log.d(TAG, "Updating UI with weather data");
        errorView.setVisibility(View.GONE);

        // Temperature
        if (weatherData.getMain() != null) {
            double temp = weatherData.getMain().getTemp();
            temperatureView.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
        } else {
            temperatureView.setText(NOT_AVAILABLE);
        }

        // Weather condition
        if (weatherData.getWeather() != null && weatherData.getWeather().length > 0) {
            String condition = weatherData.getWeather()[0].getDescription();
            // Capitalize first letter
            if (condition != null && !condition.isEmpty()) {
                condition = condition.substring(0, 1).toUpperCase(Locale.getDefault()) + condition.substring(1);
            }
            conditionView.setText(condition);
        } else {
            conditionView.setText(NOT_AVAILABLE);
        }

        // Humidity
        if (weatherData.getMain() != null) {
            int humidity = weatherData.getMain().getHumidity();
            humidityView.setText(String.format(Locale.getDefault(), "%d%%", humidity));
        } else {
            humidityView.setText(NOT_AVAILABLE);
        }

        // Wind condition
        if (weatherData.getWind() != null) {
            double speed = weatherData.getWind().getSpeed();
            double deg = weatherData.getWind().getDeg();
            String direction = getWindDirection(deg);
            windView.setText(String.format(Locale.getDefault(), "%.1f m/s %s", speed, direction));
        } else {
            windView.setText(NOT_AVAILABLE);
        }

        // Generate Gemini city image once weather data is displayed
        if (weatherData.getWeather() != null && weatherData.getWeather().length > 0) {
            String condition = weatherData.getWeather()[0].getDescription();
            if (condition != null && !condition.isEmpty()) {
                condition = condition.substring(0, 1).toUpperCase() + condition.substring(1);
            }

            // Figure out approximate time of day
            java.time.LocalTime now = java.time.LocalTime.now(clock);
            String timeOfDay;
            if (now.isBefore(java.time.LocalTime.NOON)) {
                timeOfDay = "morning";
            } else if (now.isBefore(java.time.LocalTime.of(18, 0))) {
                timeOfDay = "afternoon";
            } else {
                timeOfDay = "evening";
            }

            WeatherAwareView weatherAwareView = new WeatherAwareView(this, weatherImageView);
            weatherAwareView.generateCityImage(cityName, condition, timeOfDay);
        }
    }

    // returns wind direction
    private String getWindDirection(double degrees) {
        String[] directions = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW" };
        int index = (int) Math.round(degrees / 22.5) % 16;
        return directions[index];
    }

    // error display
    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
        temperatureView.setText(ERROR_TEXT);
        conditionView.setText(ERROR_TEXT);
        humidityView.setText(ERROR_TEXT);
        windView.setText(ERROR_TEXT);
    }
}
