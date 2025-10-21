package edu.uiuc.cs427app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.ui.AppBarConfiguration;

import edu.uiuc.cs427app.databinding.ActivityMainBinding;

import android.widget.Button;

import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private LinearLayout locationContainer;

    private void showAddLocationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add a New City");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter city name");
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String cityName = input.getText().toString().trim();
            if (!cityName.isEmpty()) {
                addCityToList(cityName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void addCityToList(String cityName){
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        android.widget.TextView cityText = new android.widget.TextView(this);
        cityText.setText(cityName);
        cityText.setTextSize(18);
        cityText.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        android.widget.Button detailsButton = new android.widget.Button(this);
        detailsButton.setText("Show Details");
        detailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("city", cityName);
            startActivity(intent);
        });
        row.addView(cityText);
        row.addView(detailsButton);
        locationContainer.addView(row);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing the UI components
        // The list of locations should be customized per user (change the implementation so that
        // buttons are added to layout programmatically
        //Button buttonChampaign = findViewById(R.id.buttonChampaign);
        //Button buttonChicago = findViewById(R.id.buttonChicago);
        //Button buttonLA = findViewById(R.id.buttonLA);
        locationContainer = findViewById(R.id.locationContainer);
        Button buttonNew = findViewById(R.id.buttonAddLocation);

        //buttonChampaign.setOnClickListener(this);
        //buttonChicago.setOnClickListener(this);
        //buttonLA.setOnClickListener(this);
        buttonNew.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.buttonAddLocation){
            showAddLocationDialog();
        }
    }
}

