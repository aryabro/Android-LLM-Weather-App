package edu.uiuc.cs427app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout locationContainer;
    private AccountManager accountManager;
    private Account account;

    private void showAddLocationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add a New City");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter city name");
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String cityName = input.getText().toString().trim();
            if (!cityName.isEmpty()) {
                addCityToList(cityName, true);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // DEBUGGING CODE
        // Retrieve the color values passed from LoginActivity
        String backgroundColor = getIntent().getStringExtra(LoginActivity.KEY_BACKGROUND_COLOR);
        String textColor = getIntent().getStringExtra(LoginActivity.KEY_TEXT_COLOR);

        // Create a message string for the toast
        String themeValuesMessage = "Theme Received:\nBG: " + backgroundColor + "\nText: " + textColor;

        // Display the toast message. LENGTH_LONG makes it stay on screen longer.
        android.widget.Toast.makeText(this, themeValuesMessage, android.widget.Toast.LENGTH_LONG).show();

        accountManager = AccountManager.get(this);

        String username = getIntent().getStringExtra("username");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Team #417 - " + username);
        }

        Account[] accounts = accountManager.getAccountsByType(LoginActivity.ACCOUNT_TYPE);
        for (Account acc : accounts) {
            if (acc.name.equals(username)) {
                account = acc;
                break;
            }
        }

        locationContainer = findViewById(R.id.locationContainer);
        Button buttonNew = findViewById(R.id.buttonAddLocation);
        Button buttonLogout = findViewById(R.id.buttonLogout);

        buttonNew.setOnClickListener(this);
        buttonLogout.setOnClickListener(this);

        String cityList = getIntent().getStringExtra(LoginActivity.KEY_CITY_LIST);
        if (cityList != null && !cityList.isEmpty()) {
            String[] cities = TextUtils.split(cityList, ",");
            for (String city : cities) {
                addCityToList(city, false);
            }
        }
    }

    private void addCityToList(String cityName, boolean save) {
        for (int i = 0; i < locationContainer.getChildCount(); i++) {
            View child = locationContainer.getChildAt(i);
            if (child instanceof LinearLayout) {
                TextView cityTextView = (TextView) ((LinearLayout) child).getChildAt(0);
                if (cityTextView.getText().toString().equals(cityName)) {
                    return;
                }
            }
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView cityText = new TextView(this);
        cityText.setText(cityName);
        cityText.setTextSize(18);
        cityText.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button detailsButton = new Button(this);
        detailsButton.setText("Show Details");
        detailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("city", cityName);
            startActivity(intent);
        });

        row.addView(cityText);
        row.addView(detailsButton);
        locationContainer.addView(row);

        if (save && account != null) {
            String currentCityList = accountManager.getUserData(account, LoginActivity.KEY_CITY_LIST);
            List<String> cities;

            if (TextUtils.isEmpty(currentCityList)) {
                cities = new ArrayList<>();
            } else {
                cities = new ArrayList<>(Arrays.asList(TextUtils.split(currentCityList, ",")));
            }

            if (!cities.contains(cityName)) {
                cities.add(cityName);
                String updatedCityList = TextUtils.join(",", cities);
                accountManager.setUserData(account, LoginActivity.KEY_CITY_LIST, updatedCityList);
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.buttonAddLocation) {
            showAddLocationDialog();
        } else if (view.getId() == R.id.buttonLogout) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}
