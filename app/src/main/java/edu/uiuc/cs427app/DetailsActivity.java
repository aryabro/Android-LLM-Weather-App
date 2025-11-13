package edu.uiuc.cs427app;

import android.os.Bundle;
import android.view.View;

public class DetailsActivity extends BaseActivity implements View.OnClickListener{

    private String cityName;
    private static final String TAG = "DetailsActivity";

    @Override
    // Called when the activity is first created.
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    // Handles click events for the views.
    public void onClick(View view) {
    }
}
