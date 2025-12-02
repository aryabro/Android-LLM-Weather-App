package edu.uiuc.cs427app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.widget.EditText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RemoveCityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> rule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;

    // prepopulates chicago into citylist for testing
    @Before
    public void setup() throws InterruptedException {
        final Object lock = new Object();

        rule.getScenario().onActivity(activity -> {
            context = activity;

            new Thread(() -> {
                // Insert city in background thread
                City chicago = new City("Chicago", 41.8781, -87.6298, "USA", "US", "USA", "", 1);
                DatabaseClient.getInstance(context).getAppDatabase().cityDao().insert(chicago);

                // Now update UI on main thread
                activity.runOnUiThread(() -> activity.addCityToUI(chicago.getId()));

                // Notify that setup is done
                synchronized (lock) {
                    lock.notify();
                }
            }).start();
        });

        // Wait until setup is finished
        synchronized (lock) {
            lock.wait();
        }
    }


    @Test
    public void testRemoveCity() throws InterruptedException {
        // 1. Confirm Chicago is displayed
        onView(withText("Chicago")).check(matches(isDisplayed()));
        Thread.sleep(600);

        // 2. Click the delete button for the Chicago row
        onView(withId(R.id.removeButton)).perform(click()); //(only works with one city)

        // 3. ASSERT: Chicago no longer appears in the list
        onView(withText("Chicago")).check(doesNotExist());
        Thread.sleep(600);
    }
}
