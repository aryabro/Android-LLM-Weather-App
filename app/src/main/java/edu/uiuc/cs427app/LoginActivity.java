package edu.uiuc.cs427app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ProgressBar; //temp for llm-ui branch

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

//    private static final String TAG = "GeminiAPI";
    public static final String ACCOUNT_TYPE = "edu.uiuc.cs427app";
    public static final String AUTH_TOKEN_TYPE = "full_access";
    public static final String KEY_CITY_LIST = "cityList";
    public static final String KEY_BACKGROUND_COLOR = "backgroundColor";
    public static final String KEY_TEXT_COLOR = "textColor";
    private ProgressBar progressBar; //temp for llm-ui branch

    private AccountManager accountManager;
    private EditText usernameEditText, passwordEditText, themeDescriptionEditText;
    private Button loginButton, signUpButton;
    private ConstraintLayout loginLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        accountManager = AccountManager.get(this);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        themeDescriptionEditText = findViewById(R.id.theme_description);
        loginButton = findViewById(R.id.login);
        signUpButton = findViewById(R.id.CreateAccountButton);
        loginLayout = findViewById(R.id.login_layout);

        loginButton.setOnClickListener(v -> signIn());
        signUpButton.setOnClickListener(v -> signUp());
    }

    private void signIn() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equals(username) && accountManager.getPassword(account).equals(password)) {
                String themeDescription = accountManager.getUserData(account, "theme_description");
                String cityList = accountManager.getUserData(account, KEY_CITY_LIST);
                String backgroundColor = accountManager.getUserData(account, KEY_BACKGROUND_COLOR);
                String textColor = accountManager.getUserData(account, KEY_TEXT_COLOR);

                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("username", username);
                intent.putExtra(KEY_CITY_LIST, cityList);

//              Pass the colors in the intent for immediate use. This will never be null anyways
//              as the default value should be applied if LLM fails during account creation.
//                if (backgroundColor != null && textColor != null) {
//                    intent.putExtra(KEY_BACKGROUND_COLOR, backgroundColor);
//                    intent.putExtra(KEY_TEXT_COLOR, textColor);
//                }

                if (themeDescription != null && !themeDescription.isEmpty()) {
                    generateTheme(themeDescription, account, intent);
                    startActivity(intent);
                    finish();
                } else {
//                    saveThemeToAccount(account,backgroundColor,textColor);
                    startActivity(intent);
                    finish();
                }
                return;
            }
        }

        Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String themeDescription = themeDescriptionEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username and password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Account account = new Account(username, ACCOUNT_TYPE);
        if (!accountManager.addAccountExplicitly(account, password, null)) {
            Toast.makeText(this, "Account with this username already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        accountManager.setAuthToken(account, AUTH_TOKEN_TYPE, password);
        accountManager.setUserData(account, "theme_description", themeDescription);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("username", username);
        intent.putExtra(KEY_CITY_LIST, "");

        if (!themeDescription.isEmpty()) {
            generateTheme(themeDescription, account, intent);
            startActivity(intent);
            finish();
        } else {
            saveDefaultThemeToAccount(account);
            startActivity(intent);
            finish();
        }
    }

    private void generateTheme(String description, Account account, Intent intent) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            Toast.makeText(this, "API key is not added. Please add it to your local.properties file.", Toast.LENGTH_LONG).show();
            saveDefaultThemeToAccount(account);
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-pro:generateContent?key=" + apiKey);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                String jsonBody = "{" +
                        "\"contents\": [{" +
                        "\"parts\":[{" +
                        "\"text\": \"Generate a UI theme based on the following description: " + description + ". " +
                        "Provide a JSON object with 'backgroundColor' and 'textColor' in hex format.\"" +
                        "}]" +
                        "}]" +
                        "}";

                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }

                    try {
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String resultText = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        int start = resultText.indexOf("{");
                        int end = resultText.lastIndexOf("}");
                        if (start != -1 && end != -1 && end > start) {
                            String jsonString = resultText.substring(start, end + 1);
                            JSONObject theme = new JSONObject(jsonString);
                            String backgroundColor = theme.getString("backgroundColor");
                            String textColor = theme.getString("textColor");

                            if (isValidColor(backgroundColor) && isValidColor(textColor)) {
                                // Save the new theme colors to the AccountManager
                                accountManager.setUserData(account, KEY_BACKGROUND_COLOR, backgroundColor);
                                accountManager.setUserData(account, KEY_TEXT_COLOR, textColor);
                            } else {
                                handler.post(() -> {
                                    Toast.makeText(LoginActivity.this, "Invalid string for theme. Using default values.", Toast.LENGTH_LONG).show();
                                    saveDefaultThemeToAccount(account);
                                });
                            }

                            handler.post(() -> {
                                saveThemeToAccount(account, backgroundColor, textColor);
                            });
                        } else {
                            handler.post(() -> {
                                Toast.makeText(LoginActivity.this, "Failed to parse theme. Using default values.", Toast.LENGTH_LONG).show();
                                saveDefaultThemeToAccount(account);
                            });
                        }
                    } catch (Exception parseException) {
                        handler.post(() -> {
                            Toast.makeText(LoginActivity.this, "Failed to parse theme. Using default values.", Toast.LENGTH_LONG).show();
                            saveDefaultThemeToAccount(account);
                        });
                    }
                } else {
                    handler.post(() -> {
                        Toast.makeText(LoginActivity.this, "Failed to generate theme. Using default values.", Toast.LENGTH_LONG).show();
                        saveDefaultThemeToAccount(account);
                    });
                }

            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(LoginActivity.this, "Failed to generate theme. Using default values.", Toast.LENGTH_LONG).show();
                    saveDefaultThemeToAccount(account);
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private boolean isValidColor(String s) {
        try {
            Color.parseColor(s);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }
    private void saveThemeToAccount(Account account, String backgroundColor, String textColor) {
        //redundant check of syntax to prevent crashes
        if (!isValidColor(backgroundColor) || !isValidColor(textColor)) {
            backgroundColor = "#FFFFFF";
            textColor = "#000000";
        }

        accountManager.setUserData(account, KEY_BACKGROUND_COLOR, backgroundColor);
        accountManager.setUserData(account, KEY_TEXT_COLOR, textColor);
    }

    private void saveDefaultThemeToAccount(Account account) {
        String defaultBackground = "#FFFFFF";  // white background
        String defaultText = "#000000";        // black text

        accountManager.setUserData(account, KEY_BACKGROUND_COLOR, defaultBackground);
        accountManager.setUserData(account, KEY_TEXT_COLOR, defaultText);
    }
}
