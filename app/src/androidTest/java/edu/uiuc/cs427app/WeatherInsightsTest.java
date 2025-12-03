package edu.uiuc.cs427app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WeatherInsightsTest {

    private static final String TEST_CITY = "Chicago";
    private static final double TEST_TEMPERATURE = 20.0;
    private static final int TEST_HUMIDITY = 65;
    private static final String TEST_CONDITION = "Clear";
    private static final double TEST_WIND_SPEED = 5.5;
    private static final double TEST_WIND_DEG = 180.0;

    private static final int QUESTION_LOAD_WAIT_MS = 5000;
    private static final int ANSWER_LOAD_WAIT_MS = 5000;
    private static final int STEPS_DEMO_WAIT = 1000;

    private ActivityScenario<WeatherInsightsActivity> scenario;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, WeatherInsightsActivity.class);
        intent.putExtra("city", TEST_CITY);
        intent.putExtra("temperature", TEST_TEMPERATURE);
        intent.putExtra("humidity", TEST_HUMIDITY);
        intent.putExtra("condition", TEST_CONDITION);
        intent.putExtra("windSpeed", TEST_WIND_SPEED);
        intent.putExtra("windDeg", TEST_WIND_DEG);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Custom matcher to select a view at a specific index when multiple views match.
     * used when there are multiple buttons in the questionsContainer.
     */
    private static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (matcher.matches(view)) {
                    if (currentIndex == index) {
                        currentIndex++;
                        return true;
                    }
                    currentIndex++;
                }
                return false;
            }
        };
    }

    /**
     * Test 1: Verify that question buttons are displayed after activity launch.
     */
    @Test
    public void testQuestionButtonsDisplayed() throws InterruptedException {
        Thread.sleep(QUESTION_LOAD_WAIT_MS);

        onView(withId(R.id.questionsContainer))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        Thread.sleep(STEPS_DEMO_WAIT);

        onView(withId(R.id.questionsContainer))
                .check(matches(hasMinimumChildCount(1)));

        Thread.sleep(STEPS_DEMO_WAIT);
    }

    /**
     * Test 2: Verify clicking a question button shows the answer section
     * and selecting another question.
     */
    @Test
    public void testClickQuestionShowsAnswer() throws InterruptedException {
        Thread.sleep(QUESTION_LOAD_WAIT_MS);

        onView(withId(R.id.questionsContainer))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        Thread.sleep(STEPS_DEMO_WAIT);

        onView(withIndex(allOf(
                instanceOf(Button.class),
                withParent(withId(R.id.questionsContainer))
        ), 0)).perform(click());

        Thread.sleep(STEPS_DEMO_WAIT);

        Thread.sleep(ANSWER_LOAD_WAIT_MS);

        onView(withId(R.id.answerLabel))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        Thread.sleep(STEPS_DEMO_WAIT);

        onView(withId(R.id.selectedQuestionText))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        Thread.sleep(STEPS_DEMO_WAIT);

        onView(withId(R.id.answerScrollView))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        Thread.sleep(STEPS_DEMO_WAIT);

        onView(withIndex(allOf(
                instanceOf(Button.class),
                withParent(withId(R.id.questionsContainer))
        ), 1)).perform(click());

        Thread.sleep(ANSWER_LOAD_WAIT_MS);

        onView(withId(R.id.answerScrollView))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    /**
     * Test 3: Verify refresh button functionality including refreshing and cooldown.
     */
    @Test
    public void testRefreshButtonFunctionality() throws InterruptedException {
        Thread.sleep(QUESTION_LOAD_WAIT_MS);

        onView(withId(R.id.questionsContainer))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        Thread.sleep(STEPS_DEMO_WAIT);

        onView(withId(R.id.refreshQuestionsButton))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()));

        Thread.sleep(STEPS_DEMO_WAIT);

        onView(withId(R.id.refreshQuestionsButton))
                .perform(click());

        Thread.sleep(500);

        onView(withId(R.id.refreshQuestionsButton))
                .check(matches(not(isEnabled())));

        Thread.sleep(STEPS_DEMO_WAIT);

        Thread.sleep(QUESTION_LOAD_WAIT_MS);

        onView(withId(R.id.questionsContainer))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        Thread.sleep(STEPS_DEMO_WAIT);

        onView(withId(R.id.questionsContainer))
                .check(matches(hasMinimumChildCount(1)));

        Thread.sleep(STEPS_DEMO_WAIT);

        Thread.sleep(17000); //refresh timer

        onView(withId(R.id.refreshQuestionsButton))
                .check(matches(isEnabled()))
                .check(matches(withText("Refresh Questions")));
    }
}

