package edu.uiuc.cs427app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DetailsActivity extends BaseActivity implements View.OnClickListener{

    private String cityName;
    private static final String TAG = "DetailsActivity";

    @Override
    // Called when the activity is first created.
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Process the Intent payload that has opened this Activity and show the information accordingly
        cityName = getIntent().getStringExtra("city");
        if (TextUtils.isEmpty(cityName)) {
            Toast.makeText(this, "City not provided.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no city is provided
            return;
        }

        String welcome = "Welcome to " + cityName;
        String cityWeatherInfo = "Detailed information about the weather of " + cityName;

        // Initializing the GUI elements
        TextView welcomeMessage = findViewById(R.id.welcomeText);
        TextView cityInfoMessage = findViewById(R.id.cityInfo);

        welcomeMessage.setText(welcome);
        cityInfoMessage.setText(cityWeatherInfo);

        Button buttonMap = findViewById(R.id.mapButton);
        buttonMap.setOnClickListener(this);

    }

    @Override
    // Handles click events for the views.
    public void onClick(View view) {
        String weatherApiKey = BuildConfig.WEATHER_API_KEY;
        if (TextUtils.isEmpty(weatherApiKey)) {
            Log.e(TAG, "Weather API key is missing.");
            Toast.makeText(DetailsActivity.this, "API Key is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        runOnUiThread(() -> Toast.makeText(DetailsActivity.this, "Fetching coordinates...", Toast.LENGTH_SHORT).show());

        new Thread(() -> {
            HttpURLConnection conn = null;
            String apiResponse = ""; // Declared here to be in scope for the catch block
            try {
                String urlString = "https://api.openweathermap.org/geo/1.0/direct?q=" + cityName + "&limit=1&appid=" + weatherApiKey;
                Log.d(TAG, "Request URL: " + urlString);
                URL url = new URL(urlString);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    apiResponse = content.toString();
                    Log.d(TAG, "SUCCESS - API Response: " + apiResponse);

                    // Now, we attempt to parse
                    JSONArray jsonArray = new JSONArray(apiResponse);
                    if (jsonArray.length() > 0) {
                        JSONObject cityObject = jsonArray.getJSONObject(0);
                        double lat = cityObject.getDouble("lat");
                        double lon = cityObject.getDouble("lon");

                        // extract lat and long coords
                        runOnUiThread(() -> {
                            Intent intent = new Intent(DetailsActivity.this, MapActivity.class);
                            intent.putExtra("city", cityName);
                            intent.putExtra("lat", lat);
                            intent.putExtra("lng", lon);
                            startActivity(intent);
                        });
                    } else {
                         runOnUiThread(() -> Toast.makeText(DetailsActivity.this, "Coordinates not found.", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e(TAG, "HTTP Error: " + responseCode);
                    runOnUiThread(() -> Toast.makeText(DetailsActivity.this, "HTTP Error: " + responseCode, Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                // This is the crucial log. It will show the exact response that caused the error.
                Log.e(TAG, "Exception during processing. Payload was: '" + apiResponse + "'", e);
                runOnUiThread(() -> Toast.makeText(DetailsActivity.this, "Error processing data.", Toast.LENGTH_LONG).show());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
}
