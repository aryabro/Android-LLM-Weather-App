package edu.uiuc.cs427app;

// Static imports for Espresso
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

// Static imports for Espresso Intents - REQUIRED
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

// Android testing
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;

// Android account management
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

// JUnit
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SignUpTest {

    private final String testUsername = "testuser_" + System.currentTimeMillis();
    private final String testPassword = "testpassword123";

    @Before
    public void setUp() {
        // Clean up any existing account with the same name before the test runs
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(LoginActivity.ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equals(testUsername)) {
                accountManager.removeAccountExplicitly(account);
            }
        }

        // Initialize Espresso Intents BEFORE launching the activity
        Intents.init();
    }

    @Test
    public void checkUserSignup() throws InterruptedException {
        // Launch the activity AFTER Intents.init() has been called
        ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class);

        // Type the username
        onView(withId(R.id.username)).perform(typeText(testUsername));
        Thread.sleep(1000);

        // Close the soft keyboard
        onView(withId(R.id.username)).perform(closeSoftKeyboard());
        Thread.sleep(1000);

        // Type the password
        onView(withId(R.id.password)).perform(typeText(testPassword));
        Thread.sleep(1000);

        // Close the soft keyboard
        onView(withId(R.id.password)).perform(closeSoftKeyboard());
        Thread.sleep(1000);

        // Click the sign-up button
        onView(withId(R.id.CreateAccountButton)).perform(click());
        Thread.sleep(1000);

        // Verify that the MainActivity is launched
        intended(hasComponent(MainActivity.class.getName()));

        // Close the scenario
        scenario.close();
    }

    @After
    public void tearDown() {
        // Release Espresso Intents
        Intents.release();

        // Clean up the test account after the test completes
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(LoginActivity.ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equals(testUsername)) {
                accountManager.removeAccountExplicitly(account);
            }
        }
    }
}