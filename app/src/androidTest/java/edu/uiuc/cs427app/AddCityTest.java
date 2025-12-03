package edu.uiuc.cs427app;

import android.widget.EditText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import static org.hamcrest.Matchers.allOf;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AddCityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> rule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testAddCity() throws InterruptedException {

        // 1. Click the "Add Location" button
        onView(withId(R.id.buttonAddLocation)).perform(click());
        Thread.sleep(800);

        // 2. Type city name in the dialog
        onView(isAssignableFrom(EditText.class))
                .perform(typeText("Chicago"), closeSoftKeyboard());
        Thread.sleep(500);

        // 3. Press "Add" in the dialog
        onView(withText("Add")).perform(click());

        // 4. Wait for DB + UI update
        Thread.sleep(2500);

        // 5. CLOSE SUCCESS DIALOG
        onView(withText("OK")).perform(click());
        Thread.sleep(800);

        // 6. ASSERT: Chicago is visible in the city list (locationContainer)
        onView(withText("Chicago")).check(matches(isDisplayed()));
    }
}
