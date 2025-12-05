package edu.uiuc.cs427app;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import android.widget.TextView;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.time.ZoneId;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.LocalTime;
import java.time.LocalDate;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;


import android.widget.Button;
import android.view.View;
import android.view.ViewParent;
import android.widget.LinearLayout;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import com.google.gson.Gson;
@RunWith(AndroidJUnit4.class)
public class MapActivityTest {
    private CityDao cityDao;
    private final Gson gson = new Gson();

    /**
     * Custom matcher to find a MAP button that is in the same container as a
     * specific city name.
     * Since the layout is:
     * cityEntry (LinearLayout VERTICAL)
     * ├── cityText (TextView) - city name
     * ├── buttonRow (LinearLayout HORIZONTAL)
     * └── mapButton (Button)
     * <p>
     * We need to find a button that is a descendant of a container that contains
     * the city name as a direct child (not in nested containers).
     */
    private static Matcher<View> mapButtonForCity(String cityName) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("MAP button for city: " + cityName);
            }

            @Override
            protected boolean matchesSafely(View item) {
                if (!(item instanceof Button)) {
                    return false;
                }
                Button button = (Button) item;
                if (!"MAP".equals(button.getText().toString())) {
                    return false;
                }

                // Find the direct parent container (buttonRow)
                ViewParent buttonRowParent = item.getParent();
                if (!(buttonRowParent instanceof View)) {
                    return false;
                }

                // Find the cityEntry (parent of buttonRow)
                ViewParent cityEntryParent = ((View) buttonRowParent).getParent();
                if (!(cityEntryParent instanceof LinearLayout)) {
                    return false;
                }

                LinearLayout cityEntry = (LinearLayout) cityEntryParent;

                // Check if cityEntry has VERTICAL orientation (cityEntry is VERTICAL, buttonRow
                // is HORIZONTAL)
                if (cityEntry.getOrientation() != LinearLayout.VERTICAL) {
                    return false;
                }

                // Check if the first child of cityEntry is a TextView with the city name
                if (cityEntry.getChildCount() > 0) {
                    View firstChild = cityEntry.getChildAt(0);
                    if (firstChild instanceof TextView) {
                        TextView cityTextView = (TextView) firstChild;
                        String text = cityTextView.getText().toString();
                        return cityName.equals(text);
                    }
                }

                return false;
            }
        };
    }

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();

        // initialize IdlingResource
        AppIdlingResource.initialize();
        IdlingResource idlingResource = (IdlingResource) AppIdlingResource.getCountingIdlingResource();
        if (idlingResource != null) {
            IdlingRegistry.getInstance().register(idlingResource);
        }

        // use DatabaseClient to get database (same instance as used by MainActivity)
        DatabaseClient dbClient = DatabaseClient.getInstance(context);
        cityDao = dbClient.getAppDatabase().cityDao();

        // clean up test data (if exists) - delete city IDs used in tests (1, 2, 3, 4)
        try {
            cityDao.deleteByIds(Arrays.asList(1, 2, 3, 4));
        } catch (Exception e) {
            // if deletion fails (e.g. method not exists), continue execution
            // this allows tests to run even if CityDao does not have delete method
        }

        // prepare test data - Chicago and New York
        City chicago = new City("Chicago", 41.8781, -87.6298, "United States", "US", "USA", "Illinois", 1);
        City newYork = new City("New York", 40.7128, -74.0060, "United States", "US", "USA", "New York", 2);

        // use insertAll to insert test data
        cityDao.insertAll(Arrays.asList(chicago, newYork));

        // initialize Espresso Intents (ensure initialized before each test)
        try {
            Intents.release(); // if previously initialized, release first
        } catch (Exception e) {
            // if not initialized, ignore exception
        }
        Intents.init();
    }

    @After
    public void tearDown() {
        // release Intents (ensure cleared)
        try {
            Intents.release();
        } catch (Exception e) {
            // if already released or not initialized, ignore exception
        }

        // unregister IdlingResource
        IdlingResource idlingResource = (IdlingResource) AppIdlingResource.getCountingIdlingResource();
        if (idlingResource != null) {
            try {
                IdlingRegistry.getInstance().unregister(idlingResource);
            } catch (Exception e) {
                // ignore exception
            }
        }
        AppIdlingResource.reset();
    }

    @Test
    public void test_1_MainToMapTransitionTest() {
        // start MainActivity, pass username and city list (city ID: 1 represents Chicago)
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra("username", "testUser");
        intent.putExtra(LoginActivity.KEY_CITY_LIST, "1"); // 传递 Chicago 的城市 ID

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent);

        try {
            Thread.sleep(2000); // wait for 2 seconds to ensure UI is fully loaded
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify MAP button exists and is visible
        onView(allOf(
                withText("MAP"),
                isDisplayed()))
                .check(matches(isDisplayed()));

        // assert: verify MAP button is clickable (enabled)
        onView(allOf(
                withText("MAP"),
                isDisplayed()))
                .check(matches(isEnabled()));

        // perform: click MAP button on Chicago item
        onView(allOf(
                withText("MAP"),
                isDisplayed())).perform(click());

        try {
            Thread.sleep(7500); // wait for 7.5 seconds to ensure the map is fully loaded
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify MapActivity is started correctly
        intended(hasComponent(MapActivity.class.getName()));

        // assert: verify Intent extras are passed correctly
        intended(allOf(
                hasExtra("city", "Chicago"),
                hasExtra("lat", 41.8781),
                hasExtra("lng", -87.6298),
                hasExtra("username", "testUser")));
    }

    @Test
    public void test_2_MapActivityDisplaysFreshData() {
        // First launch MapActivity for Chicago - pass username, city, lat, long
        Intent intent1 = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        intent1.putExtra("username", "testUser");
        intent1.putExtra("city", "Chicago");
        intent1.putExtra("lat", 41.8781);
        intent1.putExtra("lng", -87.6298);

        ActivityScenario<MapActivity> scenario1 = ActivityScenario.launch(intent1);

        // wait for map to load
        try {
            Thread.sleep(7500); // wait for 7.5 seconds to ensure the map is fully loaded
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // record first map
        // TODO

        // close MapActivity for Chicago
        scenario1.close();

        // wait for 2 sec
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Second launch MapActivity for Champaign
        Intent intent2 = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        intent2.putExtra("username", "testUser");
        intent2.putExtra("city", "New York");
        intent2.putExtra("lat", 40.7128);
        intent2.putExtra("lng", -74.0060);

        ActivityScenario<MapActivity> scenario2 = ActivityScenario.launch(intent2);

        // wait for map to load
        try {
            Thread.sleep(7500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // close MapActivity for New York
        scenario2.close();
    }
}

// No need to test zoom in/zoom out for the Map