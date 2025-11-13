package edu.uiuc.cs427app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class for making calls to the Gemini LLM API.
 * Handles async execution and JSON response parsing.
 */
public class GeminiApiClient {

    private static final String TAG = "GeminiApiClient";
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=";

    private final String apiKey;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public GeminiApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Makes an async call to Gemini API with the given prompt.
     *
     * @param prompt   The text prompt to send to Gemini
     * @param callback Callback to handle success or failure
     */
    public void generateContent(String prompt, GeminiCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            mainHandler.post(() -> callback.onFailure("API key is not configured"));
            return;
        }

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_BASE_URL + apiKey);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(30000);  // 30 seconds to connect
                conn.setReadTimeout(60000);     // 60 seconds to read response (LLM can be slow)

                // Build JSON request body
                String jsonBody = buildRequestBody(prompt);

                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line.trim());
                        }
                    }

                    String responseText = parseResponse(response.toString());
                    if (responseText != null) {
                        mainHandler.post(() -> callback.onSuccess(responseText));
                    } else {
                        mainHandler.post(() -> callback.onFailure("Failed to parse response"));
                    }
                } else {
                    String errorMsg = "API request failed with code: " + responseCode;
                    Log.e(TAG, errorMsg);
                    mainHandler.post(() -> callback.onFailure(errorMsg));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API", e);
                mainHandler.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Builds the JSON request body for Gemini API.
     */
    private String buildRequestBody(String prompt) {
        return "{"
                + "\"contents\":[{"
                + "\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]"
                + "}]"
                + "}";
    }

    /**
     * Parses the Gemini API response and extracts the text content.
     */
    private String parseResponse(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            String resultText = jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            Log.d(TAG, "Parsed response: " + resultText);
            return resultText;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse response", e);
            return null;
        }
    }

    /**
     * Escapes special characters for JSON string.
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Callback interface for Gemini API responses.
     */
    public interface GeminiCallback {
        void onSuccess(String response);

        void onFailure(String error);
    }
}
