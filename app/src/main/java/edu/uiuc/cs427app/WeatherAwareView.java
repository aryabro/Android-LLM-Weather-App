package edu.uiuc.cs427app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WeatherAwareView {
    private final Activity activity;
    private final ImageView imageView;
    private final OkHttpClient client = new OkHttpClient();

    public WeatherAwareView(Activity activity, ImageView imageView) {
        this.activity = activity;
        this.imageView = imageView;
    }


    public void generateCityImage(String cityName, String condition, String time) {

        String prompt = "Create a realistic image of " + cityName +
                " that matches this weather: " + condition +
                " in the " + time +
                ". Include lighting, reflections, and atmosphere details in a photorealistic style.";
        Log.d("WeatherAwareView", "Prompt: " + prompt);

        // Build proper JSON body
        JSONObject requestJson = new JSONObject();
        try {
            JSONArray contents = new JSONArray();

            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONObject contentObj = new JSONObject();
            contentObj.put("role", "user");
            contentObj.put("parts", new JSONArray().put(part));

            contents.put(contentObj);
            requestJson.put("contents", contents);

        } catch (JSONException e) {
            Log.e("WeatherAwareView", "JSON build error", e);
            return;
        }

        //Prepare HTTP request
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e("WeatherAwareView", "Missing Gemini API key!");
            return;
        }
        apiKey = apiKey.replace("\"", "").trim();

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-image:generateContent?key=" + apiKey;
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(requestJson.toString(), JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        //  Send request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("WeatherAwareView", "Gemini Image API request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("WeatherAwareView", "Unsuccessful response: " + response.code() + " " + response.message());
                    Log.e("WeatherAwareView", "Response body: " + response.body().string());
                    return;
                }

                String responseBody = response.body().string();
                Log.d("WeatherAwareView", "Gemini Image Response: " + responseBody);

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    JSONObject imagePart = null;

                    for (int i = 0; i < parts.length(); i++) {
                        JSONObject partObj = parts.getJSONObject(i);
                        if (partObj.has("inlineData")) {
                            imagePart = partObj.getJSONObject("inlineData");
                            break;
                        }
                    }

                    if (imagePart == null) {
                        Log.e("WeatherAwareView", "No image found in response");
                        return;
                    }

                    String base64Image = imagePart.getString("data");


                    byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    activity.runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                } catch (JSONException e) {
                    Log.e("WeatherAwareView", "Failed to parse Gemini image response", e);
                }
            }
        });
    }
}
