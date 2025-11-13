package edu.uiuc.cs427app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity {
    @SuppressLint("SetJavaScriptEnabled") // Required for Google Maps embed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        String cityName = getIntent().getStringExtra("city");
        double lat = getIntent().getDoubleExtra("lat", 0.0);
        double lng = getIntent().getDoubleExtra("lng", 0.0);

        TextView cityNameView = findViewById(R.id.map_city_name);
        TextView coordinatesView = findViewById(R.id.map_coordinates);
        WebView webView = findViewById(R.id.map_webview);

        cityNameView.setText(cityName);
        // Use String.format to avoid concatenation warnings
        coordinatesView.setText(String.format("Latitude: %s, Longitude: %s", lat, lng));

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Prevent links from opening in an external browser
        webView.setWebViewClient(new WebViewClient());

        String mapUrl = "https://maps.google.com/maps?q=" + lat + "," + lng + "&t=&z=15&ie=UTF8&iwloc=&output=embed";

        // Create an HTML string that embeds the URL in an iframe
        String htmlContent = "<!DOCTYPE html><html><head><style>html,body,iframe{height:100%;width:100%;margin:0;padding:0;border:0;}</style></head>" +
                             "<body><iframe src=\"" + mapUrl + "\"></iframe></body></html>";

        // Load the custom HTML content
        webView.loadData(htmlContent, "text/html", "UTF-8");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Map - " + cityName);
        }
    }
}
