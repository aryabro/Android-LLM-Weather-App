package edu.uiuc.cs427app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.ui.AppBarConfiguration;

import edu.uiuc.cs427app.databinding.ActivityMainBinding;
import edu.uiuc.cs427app.AppDatabase;
import edu.uiuc.cs427app.City;
import edu.uiuc.cs427app.CityDao;
import edu.uiuc.cs427app.DatabaseClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private LinearLayout locationContainer;
    private ArrayList<String> cityList;
    private static final String ACCOUNT_CITIES_KEY = "cities"; // stored in AccountManager userData
    private android.app.ProgressDialog progressDialog;
    private volatile boolean importAttempted = false;
    private AccountManager accountManager;
    private Account account;
    private String username; // Current logged-in username required for theme

    private void importCitiesFromCSV() {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(getAssets().open("worldcities.csv")));
                List<City> cities = new ArrayList<>();
                String line;
                reader.readLine(); // 跳过 header
                while ((line = reader.readLine()) != null) {
                    List<String> parts = parseCsvLine(line);
                    if (parts.size() < 7) {
                        continue;
                    }
                    // schema: city, city_ascii, country, iso2, iso3, admin_name, id
                    String city = parts.get(0).trim();
                    String city_ascii = parts.get(1).trim();
                    String country = parts.get(2).trim();
                    String iso2 = parts.get(3).trim();
                    String iso3 = parts.get(4).trim();
                    String admin_name = parts.get(5).trim();
                    int id;
                    try {
                        id = Integer.parseInt(parts.get(6).trim());
                    } catch (NumberFormatException nfe) {
                        continue;
                    }

                    cities.add(new City(city, city_ascii, country, iso2, iso3, admin_name, id));

                    if (cities.size() >= 500) {
                        DatabaseClient.getInstance(this).getAppDatabase()
                                .cityDao().insertAll(cities);
                        cities.clear();
                    }
                }
                if (!cities.isEmpty()) {
                    DatabaseClient.getInstance(this).getAppDatabase()
                            .cityDao().insertAll(cities);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }

    // this function basically will show the tab/dialogue once a user click the "Add
    // City" button
    private void showAddLocationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add a New City");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter city name");
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            CharSequence text = input.getText();
            String cityName = text == null ? null : text.toString();
            if (cityName != null) {
                cityName = cityName.trim();
            }

            if (cityName == null || cityName.isEmpty()) {
                showInputErrorDialog("Please enter a city name!");
                return;
            }

            // Basic input validation
            if (isCityAlreadyInList(cityName)) {
                showInputErrorDialog("This city already exists in your list!");
            } else if (containsNumbers(cityName)) {
                showInputErrorDialog("City names should not contain numbers. Please enter a valid city name.");
            } else {
                validateAndAddCity(cityName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // This function will show the error message once user writes the wrong message
    // input
    private void showInputErrorDialog(String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("⚠️ Input Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            // Reopen the add city dialog
            showAddLocationDialog();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();
    }

    // it tests if the input has integers
    private boolean containsNumbers(String text) {
        return text.matches(".*\\d.*");
    }
    // it tests if the city is already existed in the list

    private boolean isCityAlreadyInList(String cityName) {
        if (cityName == null) {
            return false;
        }
        String normalizedInput = cityName.trim().toLowerCase();
        for (String existingCity : cityList) {
            if (existingCity != null && existingCity.toLowerCase().equals(normalizedInput)) {
                return true;
            }
        }
        return false;
    }

    // it validate the city before writing to the city list. Otherwise, it shows
    // error message
    private void validateAndAddCity(String cityName) {
        // Show progress dialog
        progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Validating city...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                CityDao dao = DatabaseClient.getInstance(this)
                        .getAppDatabase()
                        .cityDao();

                int total = dao.countCities();
                // If database is empty, try to (re)trigger import and wait briefly
                if (total == 0) {
                    if (!importAttempted) {
                        importAttempted = true;
                        importCitiesFromCSV();
                    }
                    // Poll a few times up to ~2 seconds for import to fill DB
                    int attempts = 0;
                    while (attempts < 10) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ignored) {
                        }
                        total = dao.countCities();
                        if (total > 0)
                            break;
                        attempts++;
                    }
                    if (total == 0) {
                        runOnUiThread(() -> {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            showInputErrorDialog("Initializing city database, please try again in a moment.");
                        });
                        return;
                    }
                }

                List<City> matchedCities = dao.findAllByName(cityName);

                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    if (matchedCities == null || matchedCities.isEmpty()) {
                        showInputErrorDialog("This may be not a valid city in the real world.");
                    } else if (matchedCities.size() == 1) {
                        City city = matchedCities.get(0);
                        addCityToList(city.getCity());
                        showSuccessDialog(city.getCity());
                    } else {
                        showCityChoiceDialog(matchedCities);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    showInputErrorDialog("Unexpected error during validation. Please try again.");
                });
            }
        }).start();
    }

    private List<City> findMatchingCities(String cityName) {
        // Deprecated by background-threaded DAO queries; keep method for compatibility
        // if needed
        return new ArrayList<>();
    }

    // if user successfully add a city to the list, it will have a success dialog
    // message
    private void showSuccessDialog(String cityName) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("✅ City Added Successfully!");
        builder.setMessage("The city \"" + cityName + "\" has been added to your list.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.show();
    }

    private void showCityChoiceDialog(List<City> cities) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Multiple Cities Found");
        String[] options = new String[cities.size()];
        for (int i = 0; i < cities.size(); i++) {
            City c = cities.get(i);
            options[i] = c.getCity() + ", " + c.getCountry();
        }
        builder.setItems(options, (dialog, which) -> {
            City selectedCity = cities.get(which);
            addCityToList(selectedCity.getCity());
            showSuccessDialog(selectedCity.getCity());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // it add the city ot the list
    private void addCityToList(String cityName) {
        // Add to city list
        cityList.add(cityName);

        // Add to UI
        addCityToUI(cityName);

        // Save to preferences
        saveCityList();
    }

    // it removes the selected city from the list
    private void removeCityFromList(String cityName, LinearLayout row) {
        // Remove from city list
        cityList.remove(cityName);

        // Remove from UI
        locationContainer.removeView(row);

        // Save to preferences
        saveCityList();
    }

    // It saves your current list of cities to persistent storage
    private void saveCityList() {
        // Persist the city list to the current account so each user has their own list
        if (account != null && accountManager != null) {
            String joined = TextUtils.join(",", cityList);
            accountManager.setUserData(account, ACCOUNT_CITIES_KEY, joined);
        } else {
            // Fallback to SharedPreferences if account is unavailable

        }
    }

    private void loadCityListFromAccount() {
        cityList.clear();
        if (account != null && accountManager != null) {
            String stored = accountManager.getUserData(account, ACCOUNT_CITIES_KEY);
            if (stored != null && !stored.isEmpty()) {
                String[] cities = TextUtils.split(stored, ",");
                for (String c : cities) {
                    String name = c == null ? null : c.trim();
                    if (name != null && !name.isEmpty()) {
                        cityList.add(name);
                        addCityToUI(name);
                    }
                }
            }
        }
    }

    private void addCityToUI(String cityName) {
        // Create row layout
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setTag(cityName); // Tag for easy identification

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 20, 0,0);
        row.setLayoutParams(rowParams);

        // City name text view
        android.widget.TextView cityText = new android.widget.TextView(this);
        cityText.setText(cityName);
        cityText.setTextSize(18);
        cityText.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Programatically creating buttons, intializing colors/shape (rounded corner)
        GradientDrawable detailsButtonShape = new GradientDrawable();
        detailsButtonShape.setCornerRadius(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 100,
                        getResources().getDisplayMetrics()));
        detailsButtonShape.setColor(ContextCompat.getColor(this, R.color.purple_500));

        GradientDrawable removeButtonShape = new GradientDrawable();
        removeButtonShape.setCornerRadius(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 100,
                        getResources().getDisplayMetrics()));
        removeButtonShape.setColor(ContextCompat.getColor(this, R.color.logout_button_red));

        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4,
                getResources().getDisplayMetrics());

        int marginHorizontal = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2,
                getResources().getDisplayMetrics());

        // Details button
        Button detailsButton = new Button(this);
        detailsButton.setText("Details");
        detailsButton.setTextColor(Color.WHITE);
        detailsButton.setBackground(detailsButtonShape);
        detailsButton.setTypeface(null, Typeface.NORMAL);

        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        detailsParams.setMargins(marginHorizontal, 0, marginHorizontal, 0);
        detailsButton.setLayoutParams(detailsParams);
        detailsButton.setLayoutParams(detailsParams);
        detailsButton.setMinHeight(0);
        detailsButton.setMinimumHeight(0);
        detailsButton.setPadding(padding, padding / 2, padding, padding / 2);
        detailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("city", cityName);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // Remove button
        Button removeButton = new Button(this);
        removeButton.setBackground(removeButtonShape);
        removeButton.setTypeface(null, Typeface.NORMAL);
        removeButton.setText("Remove");
        removeButton.setTextColor(Color.WHITE);
        removeButton.setMinHeight(0);
        removeButton.setMinimumHeight(0);

        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        removeParams.setMargins(marginHorizontal, 0, marginHorizontal, 0);
        removeButton.setLayoutParams(removeParams);
        removeButton.setLayoutParams(removeParams);
        removeButton.setPadding(padding, padding / 2, padding, padding / 2);
        removeButton.setOnClickListener(v -> removeCityFromList(cityName, row));

        row.addView(cityText);
        row.addView(detailsButton);
        row.addView(removeButton);
        locationContainer.addView(row);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure city list is initialized before any usage
        cityList = new ArrayList<>();

        accountManager = AccountManager.get(this);
        username = getIntent().getStringExtra("username");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Team 417 - " + username);
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

        importCitiesFromCSV();
        importAttempted = true;

        String passedCityList = getIntent().getStringExtra(LoginActivity.KEY_CITY_LIST);
        if (passedCityList != null && !passedCityList.isEmpty()) {
            String[] cities = TextUtils.split(passedCityList, ",");
            for (String city : cities) {
                String name = city == null ? null : city.trim();
                if (name != null && !name.isEmpty()) {
                    cityList.add(name);
                    addCityToUI(name);
                }
            }
            // Ensure the passed list is saved to the current account
            saveCityList();
        } else {
            // Load from account storage when nothing is passed from LoginActivity
            loadCityListFromAccount();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up progress dialog to prevent memory leaks
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
