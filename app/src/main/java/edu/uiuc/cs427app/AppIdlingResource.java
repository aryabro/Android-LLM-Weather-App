package edu.uiuc.cs427app;

/**
 * Global IdlingResource for synchronizing asynchronous operations.
 * This class provides a singleton that can be used to track background tasks
 * like database operations, network requests, etc.
 * 
 * Uses reflection to safely work with CountingIdlingResource which is only
 * available in test builds. In production builds, all methods are no-ops.
 */
public class AppIdlingResource {
    private static final String RESOURCE_NAME = "AppIdlingResource";
    private static Object countingIdlingResource = null;
    private static boolean isInitialized = false;

    /**
     * Initializes the IdlingResource. Should be called from test setup.
     * Uses reflection to create CountingIdlingResource instance.
     */
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        try {
            Class<?> clazz = Class.forName("androidx.test.espresso.idling.CountingIdlingResource");
            countingIdlingResource = clazz.getConstructor(String.class).newInstance(RESOURCE_NAME);
            isInitialized = true;
        } catch (Exception e) {
            // CountingIdlingResource not available (production build), ignore
            isInitialized = true;
        }
    }

    /**
     * Returns the CountingIdlingResource instance.
     * Returns null in production builds where IdlingResource is not available.
     */
    public static Object getCountingIdlingResource() {
        return countingIdlingResource;
    }

    /**
     * Increments the counter when an asynchronous operation starts.
     * Call this before starting any background task.
     * No-op if IdlingResource is not initialized (production builds).
     */
    public static void increment() {
        if (countingIdlingResource != null) {
            try {
                countingIdlingResource.getClass().getMethod("increment").invoke(countingIdlingResource);
            } catch (Exception e) {
                // Ignore if method not available
            }
        }
    }

    /**
     * Decrements the counter when an asynchronous operation completes.
     * Call this after any background task finishes.
     * Only decrements if the resource is not idle (to avoid negative counts).
     * No-op if IdlingResource is not initialized (production builds).
     */
    public static void decrement() {
        if (countingIdlingResource != null) {
            try {
                boolean isIdle = (Boolean) countingIdlingResource.getClass()
                        .getMethod("isIdleNow").invoke(countingIdlingResource);
                if (!isIdle) {
                    countingIdlingResource.getClass().getMethod("decrement").invoke(countingIdlingResource);
                }
            } catch (Exception e) {
                // Ignore if method not available
            }
        }
    }

    /**
     * Resets the idling resource counter.
     * Useful for test cleanup.
     */
    public static void reset() {
        if (countingIdlingResource != null) {
            try {
                while (!(Boolean) countingIdlingResource.getClass()
                        .getMethod("isIdleNow").invoke(countingIdlingResource)) {
                    countingIdlingResource.getClass().getMethod("decrement").invoke(countingIdlingResource);
                }
            } catch (Exception e) {
                // Ignore if method not available
            }
        }
    }
}

