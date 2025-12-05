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
public class MapActivityTest {
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
}
