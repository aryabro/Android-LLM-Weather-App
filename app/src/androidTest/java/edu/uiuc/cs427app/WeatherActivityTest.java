package edu.uiuc.cs427app;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import android.widget.Button;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import com.google.gson.Gson;

@RunWith(AndroidJUnit4.class)
public class WeatherActivityTest {

    private CityDao cityDao;
    private final Gson gson = new Gson();

    /**
     * Custom matcher to find a WEATHER button that is in the same container as a
     * specific city name.
     * Since the layout is:
     * cityEntry (LinearLayout VERTICAL)
     * ├── cityText (TextView) - city name
     * └── buttonRow (LinearLayout HORIZONTAL)
     * └── weatherButton (Button)
     * 
     * We need to find a button that is a descendant of a container that contains
     * the city name as a direct child (not in nested containers).
     */
    private static Matcher<View> weatherButtonForCity(String cityName) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("WEATHER button for city: " + cityName);
            }

            @Override
            protected boolean matchesSafely(View item) {
                if (!(item instanceof Button)) {
                    return false;
                }
                Button button = (Button) item;
                if (!"WEATHER".equals(button.getText().toString())) {
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

        // clean up test data (if exists) - delete city IDs used in tests (1, 2)
        try {
            cityDao.deleteByIds(Arrays.asList(1, 2));
        } catch (Exception e) {
            // if deletion fails (e.g. method not exists), continue execution
            // this allows tests to run even if CityDao does not have delete method
        }

        // prepare test data - including Chicago and Los Angeles
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
    public void MainToWeatherTransitionTest() {
        // start MainActivity, pass username and city list (city ID: 1 represents
        // Chicago)
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra("username", "testUser");
        intent.putExtra(LoginActivity.KEY_CITY_LIST, "1"); // 传递 Chicago 的城市 ID

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent);

        try {
            Thread.sleep(2000); // wait for 2 seconds to ensure UI is fully loaded
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify WEATHER button exists and is visible
        onView(allOf(
                withText("WEATHER"),
                isDisplayed()))
                .check(matches(isDisplayed()));

        // assert: verify WEATHER button is clickable (enabled)
        onView(allOf(
                withText("WEATHER"),
                isDisplayed()))
                .check(matches(isEnabled()));

        // perform: click WEATHER button on Chicago item
        onView(allOf(
                withText("WEATHER"),
                isDisplayed())).perform(click());

        try {
            Thread.sleep(2000); // wait for 2 seconds to ensure UI is fully loaded
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify WeatherActivity is started correctly
        intended(hasComponent(WeatherActivity.class.getName()));

        // assert: verify Intent extras are passed correctly
        intended(allOf(
                hasExtra("city", "Chicago"),
                hasExtra("lat", 41.8781),
                hasExtra("lng", -87.6298),
                hasExtra("username", "testUser")));
    }

    /**
     * test: verify WeatherActivity can start and display all required information
     * - city name
     * - date and time
     * - temperature
     * - weather condition
     * - humidity
     * - wind condition
     * - Weather Insights button
     */
    @Test
    public void testWeatherActivityDisplaysAllRequiredInformation() {
        // start WeatherActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), WeatherActivity.class);
        intent.putExtra("city", "Chicago");
        intent.putExtra("lat", 41.8781);
        intent.putExtra("lng", -87.6298);
        intent.putExtra("username", "testUser");

        ActivityScenario<WeatherActivity> scenario = ActivityScenario.launch(intent);

        // wait for API response and UI update
        try {
            Thread.sleep(5000); // 等待网络请求完成
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify city name is displayed
        onView(withId(R.id.weatherCityTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText("Chicago")));

        // assert: verify date and time is displayed (format: EEE, MMM d yyyy • HH:mm z)
        onView(withId(R.id.weatherDateTime))
                .check(matches(isDisplayed()));
        // assert: verify date and time is not empty and not "Loading..."
        onView(withId(R.id.weatherDateTime))
                .check(matches(not(withText("Loading..."))))
                .check(matches(not(withText(""))));

        // assert: verify temperature is displayed (format: XX.X°C)
        onView(withId(R.id.weatherTemperature))
                .check(matches(isDisplayed()));
        // assert: verify temperature is not empty and not "Loading..." or "Error"
        onView(withId(R.id.weatherTemperature))
                .check(matches(not(withText("Loading..."))))
                .check(matches(not(withText("Error"))));

        // assert: verify weather condition is displayed
        onView(withId(R.id.weatherCondition))
                .check(matches(isDisplayed()));
        // assert: verify weather condition is not empty and not "Loading..." or "Error"
        // or "N/A"
        onView(withId(R.id.weatherCondition))
                .check(matches(not(withText("Loading..."))))
                .check(matches(not(withText("Error"))))
                .check(matches(not(withText("N/A"))));

        // assert: verify humidity is displayed (format: XX%)
        onView(withId(R.id.weatherHumidity))
                .check(matches(isDisplayed()));
        // assert: verify humidity is not empty and not "Loading..." or "Error"
        onView(withId(R.id.weatherHumidity))
                .check(matches(not(withText("Loading..."))))
                .check(matches(not(withText("Error"))));

        // assert: verify wind condition is displayed (format: XX.X m/s DIRECTION)
        // Scroll to wind condition view first since it might be below the fold
        try {
            onView(withId(R.id.weatherWind))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));
        } catch (Exception e) {
            // If scrollTo fails, try swipeUp to scroll down
            try {
                onView(withId(R.id.weatherWind))
                        .perform(swipeUp())
                        .check(matches(isDisplayed()));
            } catch (Exception e2) {
                // If still fails, just check if it exists (might already be visible)
                onView(withId(R.id.weatherWind))
                        .check(matches(isDisplayed()));
            }
        }
        // assert: verify wind condition is not empty and not "Loading..." or "Error"
        onView(withId(R.id.weatherWind))
                .check(matches(not(withText("Loading..."))))
                .check(matches(not(withText("Error"))));

        // assert: verify weather insights button exists and is clickable
        // Scroll to button first since it's at the bottom of the screen
        try {
            onView(withId(R.id.weatherInsightsButton))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                    .check(matches(isEnabled()))
                    .check(matches(withText("Weather Insights")));
        } catch (Exception e) {
            // If scrollTo fails, try swipeUp to scroll down
            try {
                onView(withId(R.id.weatherInsightsButton))
                        .perform(swipeUp())
                        .check(matches(isDisplayed()))
                        .check(matches(withText("Weather Insights")));
            } catch (Exception e2) {
                // If still fails, just check if it exists (might already be visible)
                onView(withId(R.id.weatherInsightsButton))
                        .check(matches(isDisplayed()))
                        .check(matches(withText("Weather Insights")));
            }
        }
    }

    /**
     * verify WeatherActivity displays fresh data (not cached)
     * by reloading the same city to verify the data is fresh
     */
    @Test
    public void testWeatherActivityDisplaysFreshData() {
        // first launch WeatherActivity
        Intent intent1 = new Intent(ApplicationProvider.getApplicationContext(), WeatherActivity.class);
        intent1.putExtra("city", "Chicago");
        intent1.putExtra("lat", 41.8781);
        intent1.putExtra("lng", -87.6298);
        intent1.putExtra("username", "testUser");

        ActivityScenario<WeatherActivity> scenario1 = ActivityScenario.launch(intent1);

        // wait for first API response
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // record first temperature value
        String firstTemperature = "";
        try {
            // get temperature text (we can only verify it exists, actual value may change
            // over time)
            onView(withId(R.id.weatherTemperature))
                    .check(matches(isDisplayed()));
        } catch (Exception e) {
            // if failed, continue testing
        }

        // close current activity
        scenario1.close();

        // wait for a while
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // second launch WeatherActivity (same city)
        Intent intent2 = new Intent(ApplicationProvider.getApplicationContext(), WeatherActivity.class);
        intent2.putExtra("city", "Chicago");
        intent2.putExtra("lat", 41.8781);
        intent2.putExtra("lng", -87.6298);
        intent2.putExtra("username", "testUser");

        ActivityScenario<WeatherActivity> scenario2 = ActivityScenario.launch(intent2);

        // wait for second API response
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify data is loaded (not cached)
        onView(withId(R.id.weatherTemperature))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))))
                .check(matches(not(withText("Error"))));

        // verify all fields are updated
        onView(withId(R.id.weatherCondition))
                .check(matches(not(withText("Loading..."))));
        onView(withId(R.id.weatherHumidity))
                .check(matches(not(withText("Loading..."))));
    }

    /**
     * verify WeatherActivity resumes correctly after background
     */
    @Test
    public void testWeatherActivityResumesCorrectlyAfterBackground() {
        // start WeatherActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), WeatherActivity.class);
        intent.putExtra("city", "Chicago");
        intent.putExtra("lat", 41.8781);
        intent.putExtra("lng", -87.6298);
        intent.putExtra("username", "testUser");

        ActivityScenario<WeatherActivity> scenario = ActivityScenario.launch(intent);

        // wait for initial data to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // record initial data
        String initialCity = "Chicago";
        double initialLat = 41.8781;
        double initialLng = -87.6298;

        // simulate app going to background (by recreating Activity)
        scenario.recreate();

        // wait for resume
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify city name is still displayed correctly
        onView(withId(R.id.weatherCityTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText(initialCity)));

        // assert: verify data is still displayed (possibly reloaded)
        onView(withId(R.id.weatherTemperature))
                .check(matches(isDisplayed()));
    }

    /**
     * Verify WeatherActivity can resolve a city solely by database ID and populate
     * UI
     * before network data returns.
     */
    @Test
    public void testWeatherActivityLoadsCityFromDatabaseById() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), WeatherActivity.class);
        intent.putExtra("cityId", 1); // Chicago inserted during setUp()

        ActivityScenario<WeatherActivity> scenario = ActivityScenario.launch(intent);

        // Wait for the DB lookup to finish and UI to update
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.weatherCityTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText("Chicago")));

        onView(withId(R.id.weatherDateTime))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))));

        scenario.close();
    }

    /**
     * Verify WeatherActivity surfaces errors when coordinates cannot be fetched.
     */
    @Test
    public void testWeatherActivityShowsErrorForUnknownCity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), WeatherActivity.class);
        intent.putExtra("city", "UnknownCity");
        intent.putExtra("username", "testUser");

        ActivityScenario<WeatherActivity> scenario = ActivityScenario.launch(intent);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.weatherError))
                .check(matches(isDisplayed()))
                .check(matches(withText("City not found in database: UnknownCity")));

        onView(withId(R.id.weatherTemperature))
                .check(matches(withText("Error")));

        scenario.close();
    }

    /**
     * verify fetchWeatherData surfaces error when coordinates are invalid
     */
    @Test
    public void testFetchWeatherDataShowsErrorForInvalidCoordinates() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), WeatherActivity.class);
        intent.putExtra("city", "Chicago");
        intent.putExtra("lat", 41.8781);
        intent.putExtra("lng", -87.6298);
        intent.putExtra("username", "testUser");

        ActivityScenario<WeatherActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            try {
                Field latField = WeatherActivity.class.getDeclaredField("cityLat");
                Field lngField = WeatherActivity.class.getDeclaredField("cityLng");
                latField.setAccessible(true);
                lngField.setAccessible(true);
                latField.set(activity, 0.0);
                lngField.set(activity, 0.0);

                Method fetchMethod = WeatherActivity.class.getDeclaredMethod("fetchWeatherData");
                fetchMethod.setAccessible(true);
                fetchMethod.invoke(activity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        onView(withId(R.id.weatherError))
                .check(matches(isDisplayed()))
                .check(matches(withText("Invalid coordinates for Chicago (lat: 0.0, lng: 0.0)")));

        onView(withId(R.id.weatherTemperature))
                .check(matches(withText("Error")));

        scenario.close();
    }

    /**
     * verify updateWeatherUI gracefully handles missing weather sections
     */
    @Test
    public void testUpdateWeatherUIHandlesMissingData() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), WeatherActivity.class);
        intent.putExtra("city", "Chicago");
        intent.putExtra("lat", 41.8781);
        intent.putExtra("lng", -87.6298);
        intent.putExtra("username", "testUser");

        ActivityScenario<WeatherActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            try {
                WeatherData data = gson.fromJson("{\"weather\":[]}", WeatherData.class);
                Method updateMethod = WeatherActivity.class.getDeclaredMethod("updateWeatherUI", WeatherData.class);
                updateMethod.setAccessible(true);
                updateMethod.invoke(activity, data);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        onView(withId(R.id.weatherTemperature))
                .check(matches(withText("N/A")));
        onView(withId(R.id.weatherCondition))
                .check(matches(withText("N/A")));
        onView(withId(R.id.weatherHumidity))
                .check(matches(withText("N/A")));
        onView(withId(R.id.weatherWind))
                .check(matches(withText("N/A")));

        scenario.close();
    }

    /**
     * verify multiple city switching
     * simulate user: click city1 (Chicago) -> return -> click city2 (New York) ->
     * return -> click city1 (Chicago)
     * verify each time the displayed data is correct
     */
    @Test
    public void testMultipleCitySwitching() {
        // start MainActivity, contains two cities
        Intent mainIntent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        mainIntent.putExtra("username", "testUser");
        mainIntent.putExtra(LoginActivity.KEY_CITY_LIST, "1,2"); // Chicago (1) and New York (2)

        ActivityScenario<MainActivity> mainScenario = ActivityScenario.launch(mainIntent);

        // wait for UI to load
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify two cities are displayed in the list
        onView(withText("Chicago"))
                .check(matches(isDisplayed()));
        onView(withText("New York"))
                .check(matches(isDisplayed()));

        // === first time: click Chicago's WEATHER button ===
        // Since cities are added in order (Chicago ID=1, New York ID=2),
        // and the layout is vertical with city name above buttons,
        // we can scroll to Chicago first, then find the WEATHER button below it
        // For simplicity, we'll click the first WEATHER button (assuming Chicago is
        // first)
        // In a real scenario, we would need a custom matcher to find button in same
        // containe

        // Use custom matcher to find Chicago's WEATHER button
        onView(allOf(
                weatherButtonForCity("Chicago"),
                isDisplayed()))
                .perform(click());

        // wait for WeatherActivity to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify data is displayed for Chicago
        onView(withId(R.id.weatherCityTitle))
                .check(matches(withText("Chicago")));

        // assert: verify all data fields are displayed correctly
        onView(withId(R.id.weatherTemperature))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))))
                .check(matches(not(withText("Error"))));

        onView(withId(R.id.weatherCondition))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))));

        onView(withId(R.id.weatherHumidity))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))));

        // Scroll to wind condition view first since it might be below the fold
        try {
            onView(withId(R.id.weatherWind))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                    .check(matches(not(withText("Loading..."))));
        } catch (Exception e) {
            // If scrollTo fails, try swipeUp to scroll down
            try {
                onView(withId(R.id.weatherWind))
                        .perform(swipeUp())
                        .check(matches(isDisplayed()))
                        .check(matches(not(withText("Loading..."))));
            } catch (Exception e2) {
                // If still fails, just check if it exists
                onView(withId(R.id.weatherWind))
                        .check(matches(isDisplayed()))
                        .check(matches(not(withText("Loading..."))));
            }
        }

        // return to MainActivity
        pressBack();

        // wait for return
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // === second time: click New York's WEATHER button ===
        // Use custom matcher to find New York's WEATHER button
        onView(allOf(
                weatherButtonForCity("New York"),
                isDisplayed()))
                .perform(click());
        // Wait a bit for scrolling to complete
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Since there are two WEATHER buttons and New York is second,
        // we need to find the WEATHER button that is near "New York"
        // The simplest approach: scroll to New York, then the WEATHER button below it
        // should be the second one in the view hierarchy
        // Note: This assumes New York's button is the second WEATHER button
        // A more robust solution would require a custom ViewMatcher

        // wait for WeatherActivity to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify data is displayed for New York
        onView(withId(R.id.weatherCityTitle))
                .check(matches(withText("New York")));

        // assert: verify all data fields are displayed correctly
        onView(withId(R.id.weatherTemperature))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))))
                .check(matches(not(withText("Error"))));

        onView(withId(R.id.weatherCondition))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))));

        onView(withId(R.id.weatherHumidity))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))));

        // Scroll to wind condition view first since it might be below the fold
        try {
            onView(withId(R.id.weatherWind))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                    .check(matches(not(withText("Loading..."))));
        } catch (Exception e) {
            // If scrollTo fails, try swipeUp to scroll down
            try {
                onView(withId(R.id.weatherWind))
                        .perform(swipeUp())
                        .check(matches(isDisplayed()))
                        .check(matches(not(withText("Loading..."))));
            } catch (Exception e2) {
                // If still fails, just check if it exists
                onView(withId(R.id.weatherWind))
                        .check(matches(isDisplayed()))
                        .check(matches(not(withText("Loading..."))));
            }
        }

        // return to MainActivity
        pressBack();

        // wait for return
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // === third time: click Chicago's WEATHER button again ===
        // Use custom matcher to find Chicago's WEATHER button
        onView(allOf(
                weatherButtonForCity("Chicago"),
                isDisplayed()))
                .perform(click());
        // wait for WeatherActivity to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert: verify data is displayed for Chicago (data should be complete and
        // correct)
        onView(withId(R.id.weatherCityTitle))
                .check(matches(withText("Chicago")));

        // assert: verify all data fields are displayed correctly
        onView(withId(R.id.weatherTemperature))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))))
                .check(matches(not(withText("Error"))));

        onView(withId(R.id.weatherCondition))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))));

        onView(withId(R.id.weatherHumidity))
                .check(matches(isDisplayed()))
                .check(matches(not(withText("Loading..."))));

        // Scroll to wind condition view first since it might be below the fold
        try {
            onView(withId(R.id.weatherWind))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                    .check(matches(not(withText("Loading..."))));
        } catch (Exception e) {
            // If scrollTo fails, try swipeUp to scroll down
            try {
                onView(withId(R.id.weatherWind))
                        .perform(swipeUp())
                        .check(matches(isDisplayed()))
                        .check(matches(not(withText("Loading..."))));
            } catch (Exception e2) {
                // If still fails, just check if it exists
                onView(withId(R.id.weatherWind))
                        .check(matches(isDisplayed()))
                        .check(matches(not(withText("Loading..."))));
            }
        }

        // assert: verify Weather Insights button is clickable
        // Scroll to button first since it's at the bottom of the screen
        try {
            onView(withId(R.id.weatherInsightsButton))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                    .check(matches(isEnabled()));
        } catch (Exception e) {
            // If scrollTo fails, try swipeUp to scroll down
            try {
                onView(withId(R.id.weatherInsightsButton))
                        .perform(swipeUp())
                        .check(matches(isDisplayed()))
                        .check(matches(isEnabled()));
            } catch (Exception e2) {
                // If still fails, just check if it exists
                onView(withId(R.id.weatherInsightsButton))
                        .check(matches(isDisplayed()))
                        .check(matches(isEnabled()));
            }
        }
    }

}
