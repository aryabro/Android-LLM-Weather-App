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
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
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

    private static final String TAG = "GeminiAPI";
    public static final String ACCOUNT_TYPE = "edu.uiuc.cs427app";
    public static final String AUTH_TOKEN_TYPE = "full_access";
    public static final String KEY_CITY_LIST = "cityList";

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

        applyDefaultTheme();

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

                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("username", username);
                intent.putExtra(KEY_CITY_LIST, cityList);

                if (themeDescription != null && !themeDescription.isEmpty()) {
                    generateTheme(themeDescription, intent);
                } else {
                    applyDefaultTheme();
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
            generateTheme(themeDescription, intent);
        } else {
            startActivity(intent);
            finish();
        }
    }

    private void generateTheme(String description, Intent intent) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            Toast.makeText(this, "API key is not set. Please add it to your local.properties file.", Toast.LENGTH_LONG).show();
            applyDefaultTheme();
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent?key=" + apiKey);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject textPart = new JSONObject();
                textPart.put("text", "Generate a UI theme based on the following description: " + description + ". " +
                        "Provide a JSON object with 'backgroundColor' and 'textColor' in hex format.");

                JSONObject content = new JSONObject();
                content.put("parts", new JSONArray().put(textPart));

                JSONObject jsonRequestBody = new JSONObject();
                jsonRequestBody.put("contents", new JSONArray().put(content));

                String jsonBody = jsonRequestBody.toString();

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

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String resultText = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    String jsonString = resultText.replace("```json", "").replace("```", "").trim();
                    JSONObject theme = new JSONObject(jsonString);
                    String backgroundColor = theme.getString("backgroundColor");
                    String textColor = theme.getString("textColor");

                    handler.post(() -> {
                        applyTheme(backgroundColor, textColor);
                        startActivity(intent);
                        finish();
                    });

                } else {
                    handler.post(() -> {
                        Toast.makeText(LoginActivity.this, "Failed to generate theme. Applying default.", Toast.LENGTH_LONG).show();
                        applyDefaultTheme();
                        startActivity(intent);
                        finish();
                    });
                }

            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(LoginActivity.this, "Failed to generate theme. Applying default.", Toast.LENGTH_LONG).show();
                    applyDefaultTheme();
                    startActivity(intent);
                    finish();
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void applyTheme(String backgroundColor, String textColor) {
        loginLayout.setBackgroundColor(Color.parseColor(backgroundColor));
        usernameEditText.setTextColor(Color.parseColor(textColor));
        passwordEditText.setTextColor(Color.parseColor(textColor));
        themeDescriptionEditText.setTextColor(Color.parseColor(textColor));
        loginButton.setBackgroundColor(Color.parseColor(textColor));
        loginButton.setTextColor(Color.parseColor(backgroundColor));
    }

    private void applyDefaultTheme() {
        loginLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        usernameEditText.setTextColor(Color.parseColor("#000000"));
        passwordEditText.setTextColor(Color.parseColor("#000000"));
        themeDescriptionEditText.setTextColor(Color.parseColor("#000000"));
        loginButton.setBackgroundColor(Color.parseColor("#6200EE"));
        loginButton.setTextColor(Color.parseColor("#FFFFFF"));
    }
}
