#!/usr/bin/env python3
"""
Generate Espresso test for user sign-up functionality using Gemini API.

This script reads the LoginActivity and layout XML, constructs a detailed prompt
for Gemini, and generates an Espresso instrumented test that follows the project
requirements for Iteration 5.

Requirements:
- google-generativeai library: pip install google-generativeai
- GEMINI_API_KEY environment variable set

Usage:
    export GEMINI_API_KEY="your-api-key-here"
    python generate_signup_test.py
"""

import os
import sys
import re
from pathlib import Path

try:
    import google.generativeai as genai
except ImportError:
    print("Error: google-generativeai library not found.")
    print("Please install it with: pip install google-generativeai")
    sys.exit(1)


def read_file(file_path):
    """Read and return the contents of a file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()
    except FileNotFoundError:
        print(f"Error: File not found: {file_path}")
        sys.exit(1)
    except Exception as e:
        print(f"Error reading file {file_path}: {e}")
        sys.exit(1)


def extract_java_code(response_text):
    """Extract Java code from Gemini's response, handling markdown formatting."""
    # First, try to extract code from markdown code blocks
    code_block_pattern = r'```java\n(.*?)```'
    matches = re.findall(code_block_pattern, response_text, re.DOTALL)
    if matches:
        return matches[0].strip()

    # If no markdown blocks, try to find code between generic code blocks
    code_block_pattern = r'```\n(.*?)```'
    matches = re.findall(code_block_pattern, response_text, re.DOTALL)
    if matches:
        return matches[0].strip()

    # If no code blocks, return the entire response (might already be clean Java code)
    return response_text.strip()


def construct_prompt(login_activity_code, layout_xml_code, main_activity_snippet):
    """Construct a detailed prompt for Gemini following the paper's methodology."""

    prompt = f"""You are an expert Android test engineer specializing in Espresso instrumented tests. Your task is to generate a complete, production-ready Espresso test class for the user sign-up functionality.

## CODE CONTEXT (CC Part)

### Focal Class: LoginActivity
File: app/src/main/java/edu/uiuc/cs427app/LoginActivity.java

```java
{login_activity_code}
```

### Layout XML
File: app/src/main/res/layout/activity_login.xml

```xml
{layout_xml_code}
```

### Target Activity (MainActivity) - Navigation Target
File: app/src/main/java/edu/uiuc/cs427app/MainActivity.java

```java
{main_activity_snippet}
```

### Project Dependencies (from build.gradle):
```gradle
// Testing dependencies available
testImplementation 'junit:junit:4.13.2'
androidTestImplementation 'androidx.test.ext:junit:1.3.0'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.7.0'
androidTestImplementation 'androidx.test.espresso:espresso-intents:3.7.0'  // IMPORTANT: Use this for Intents
```

### Key Information:
- **Focal Method**: `signUp()` (lines 127-164 in LoginActivity.java)
- **Account Type**: "edu.uiuc.cs427app" (defined in LoginActivity.ACCOUNT_TYPE)
- **Storage Mechanism**: Android AccountManager (NOT Firebase, NOT SQLite)
- **Required Fields**:
  - Username (EditText with id: R.id.username)
  - Password (EditText with id: R.id.password)
  - Theme description is OPTIONAL (we will NOT test it)
- **Sign-up Button**: R.id.CreateAccountButton
- **Success Navigation**: LoginActivity → MainActivity (package: edu.uiuc.cs427app.MainActivity)
- **Package Name**: edu.uiuc.cs427app

## TASK DESCRIPTION (NL Part)

Generate a complete Espresso instrumented test class that meets these EXACT requirements:

### Test Class Requirements:
1. **Class Name**: `SignUpTest`
2. **Test Method Name**: `checkUserSignup` (EXACTLY this name - grading requirement)
3. **Package**: `edu.uiuc.cs427app`
4. **Location**: app/src/androidTest/java/edu/uiuc/cs427app/SignUpTest.java

### Test Structure Requirements:
1. Use `@RunWith(AndroidJUnit4.class)` annotation
2. Include `@Rule` for `ActivityScenarioRule<LoginActivity>`
3. Include `@Before` method named `setUp()` that:
   - Initializes Espresso Intents with `Intents.init()`
   - Cleans up any existing test account from AccountManager
4. Include `@After` method named `tearDown()` that:
   - Releases Espresso Intents with `Intents.release()`
   - Cleans up the test account created during the test

### Test Method Requirements (`checkUserSignup`):
1. **Actions** (with 1-second pauses between each for video recording):
   a. Type a test username into R.id.username
   b. Sleep for 1000ms: `Thread.sleep(1000);`
   c. Close the soft keyboard
   d. Sleep for 1000ms: `Thread.sleep(1000);`
   e. Type a test password into R.id.password
   f. Sleep for 1000ms: `Thread.sleep(1000);`
   g. Close the soft keyboard
   h. Sleep for 1000ms: `Thread.sleep(1000);`
   i. Click the sign-up button (R.id.CreateAccountButton)
   j. Sleep for 1000ms: `Thread.sleep(1000);`

2. **Assertions** (verify navigation to MainActivity):
   - Use `intended(hasComponent(MainActivity.class.getName()))` to verify MainActivity was launched
   - This satisfies the requirement for "assertions to check behavior against ground truth"

3. **Test Data**:
   - Use unique test credentials (e.g., "testuser_" + timestamp for uniqueness)
   - Use "testpassword123" or similar for password

### Required Imports (CRITICAL - Must include ALL of these):
```java
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
import androidx.test.ext.junit.rules.ActivityScenarioRule;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
```

### AccountManager Cleanup Logic:
In `setUp()` and `tearDown()`, use code like:
```java
Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
AccountManager accountManager = AccountManager.get(context);
Account[] accounts = accountManager.getAccountsByType("edu.uiuc.cs427app");
for (Account account : accounts) {{
    if (account.name.equals(testUsername)) {{
        accountManager.removeAccountExplicitly(account);
    }}
}}
```

### Important Notes:
1. DO NOT test the theme_description field - leave it empty
2. DO NOT add assertions for AccountManager - only check navigation
3. Include `throws InterruptedException` in the test method signature for Thread.sleep
4. Make sure all Thread.sleep() calls are exactly 1000ms
5. Use `Espresso.closeSoftKeyboard()` after typing text to ensure UI is ready

### Output Format:
Provide ONLY the complete Java test class code. Do not include:
- Markdown formatting (no ```java blocks)
- Explanations or comments outside the code
- Additional text before or after the code

Generate the complete, runnable test class now:"""

    return prompt


def main():
    print("=" * 70)
    print("Gemini Sign-Up Test Generator for CS427 Iteration 5")
    print("=" * 70)
    print()

    # Check for API key
    api_key = os.getenv('GEMINI_API_KEY')
    if not api_key:
        print("Error: GEMINI_API_KEY environment variable not set.")
        print("Please set it with: export GEMINI_API_KEY='your-api-key-here'")
        sys.exit(1)

    print("✓ API key found in environment")

    # Define file paths
    project_root = Path(__file__).parent
    login_activity_path = project_root / "app/src/main/java/edu/uiuc/cs427app/LoginActivity.java"
    layout_xml_path = project_root / "app/src/main/res/layout/activity_login.xml"
    main_activity_path = project_root / "app/src/main/java/edu/uiuc/cs427app/MainActivity.java"
    output_test_path = project_root / "app/src/androidTest/java/edu/uiuc/cs427app/SignUpTest.java"

    # Verify input files exist
    if not login_activity_path.exists():
        print(f"Error: LoginActivity.java not found at {login_activity_path}")
        sys.exit(1)
    print(f"✓ Found LoginActivity.java")

    if not layout_xml_path.exists():
        print(f"Error: activity_login.xml not found at {layout_xml_path}")
        sys.exit(1)
    print(f"✓ Found activity_login.xml")

    if not main_activity_path.exists():
        print(f"Error: MainActivity.java not found at {main_activity_path}")
        sys.exit(1)
    print(f"✓ Found MainActivity.java")

    # Read source files
    print("\nReading source files...")
    login_activity_code = read_file(login_activity_path)
    layout_xml_code = read_file(layout_xml_path)
    main_activity_code = read_file(main_activity_path)

    # Extract first 50 lines of MainActivity for context (class declaration)
    main_activity_lines = main_activity_code.split('\n')[:50]
    main_activity_snippet = '\n'.join(main_activity_lines) + '\n    // ... rest of MainActivity ...'

    print(f"✓ Read {len(login_activity_code)} characters from LoginActivity.java")
    print(f"✓ Read {len(layout_xml_code)} characters from activity_login.xml")
    print(f"✓ Read {len(main_activity_snippet)} characters from MainActivity.java (snippet)")

    # Configure Gemini
    print("\nConfiguring Gemini API...")
    genai.configure(api_key=api_key)

    # Use Gemini 2.5 Pro for high quality test generation
    model = genai.GenerativeModel('gemini-2.5-pro')
    print("✓ Using model: gemini-2.5-pro")

    # Construct prompt
    print("\nConstructing prompt with code context...")
    prompt = construct_prompt(login_activity_code, layout_xml_code, main_activity_snippet)
    print(f"✓ Prompt constructed ({len(prompt)} characters)")

    # Generate test
    print("\nGenerating test with Gemini API...")
    print("(This may take 10-30 seconds...)")
    try:
        response = model.generate_content(prompt)
        generated_code = response.text
        print("✓ Received response from Gemini")
    except Exception as e:
        print(f"Error calling Gemini API: {e}")
        sys.exit(1)

    # Extract Java code
    print("\nExtracting Java code from response...")
    java_code = extract_java_code(generated_code)
    print(f"✓ Extracted {len(java_code)} characters of Java code")

    # Ensure output directory exists
    output_test_path.parent.mkdir(parents=True, exist_ok=True)

    # Save to file
    print(f"\nSaving test to {output_test_path}...")
    try:
        with open(output_test_path, 'w', encoding='utf-8') as f:
            f.write(java_code)
        print("✓ Test file saved successfully!")
    except Exception as e:
        print(f"Error saving file: {e}")
        sys.exit(1)

    # Print summary
    print("\n" + "=" * 70)
    print("SUCCESS! Test generated and saved.")
    print("=" * 70)
    print(f"\nTest Location: {output_test_path}")
    print(f"Test Class: SignUpTest")
    print(f"Test Method: checkUserSignup")
    print(f"\nNext Steps:")
    print(f"1. Review the generated test in Android Studio")
    print(f"2. Build the project to verify compilation")
    print(f"3. Run the test: ./gradlew connectedAndroidTest")
    print(f"4. Record video of test execution for submission")
    print(f"\nNote: You may need to adjust imports or add dependencies if compilation fails.")
    print("=" * 70)


if __name__ == "__main__":
    main()
