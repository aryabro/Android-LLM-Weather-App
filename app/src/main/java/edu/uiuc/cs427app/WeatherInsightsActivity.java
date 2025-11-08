package edu.uiuc.cs427app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import edu.uiuc.cs427app.City;
import edu.uiuc.cs427app.CityDao;
import edu.uiuc.cs427app.DatabaseClient;

public class WeatherInsightsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_insights);

        // Try to get cityId first, then fall back to city name
        int cityId = getIntent().getIntExtra("cityId", -1);
        String cityName = getIntent().getStringExtra("city");

        TextView title = findViewById(R.id.insightsTitle);
        TextView content = findViewById(R.id.insightsContent);

        // If cityId is provided, fetch city info from database
        if (cityId != -1) {
            fetchCityFromDatabaseById(cityId, title, content);
        } else if (cityName != null) {
            // Use city name directly
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Insights - " + cityName);
            }
            title.setText("Weather insights for " + cityName);
            content.setText("Coming soon");
        } else {
            // No city information provided
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Weather Insights");
            }
            title.setText("Weather insights");
            content.setText("City information not available");
        }
    }

    private void fetchCityFromDatabaseById(int cityId, TextView titleView, TextView contentView) {
        new Thread(() -> {
            try {
                CityDao dao = DatabaseClient.getInstance(this).getAppDatabase().cityDao();
                City city = dao.findById(cityId);
                if (city != null) {
                    String cityName = city.getCity();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Insights - " + cityName);
                        }
                        titleView.setText("Weather insights for " + cityName);
                        contentView.setText("Coming soon");
                    });
                } else {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Weather Insights");
                        }
                        titleView.setText("Weather insights");
                        contentView.setText("City not found");
                    });
                }
            } catch (Exception e) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Weather Insights");
                    }
                    titleView.setText("Weather insights");
                    contentView.setText("Error loading city information");
                });
            }
        }).start();
    }
}
