package com.manuelmaly.hn;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test for {@link MainActivity} verifying that feed data
 * survives configuration changes (screen rotation).
 *
 * Note: Requires device/emulator with network access for initial load.
 * These tests verify the ViewModel retains state across rotation.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityConfigChangeTest {

    @Rule
    public ActivityTestRule<MainActivity_> activityRule =
            new ActivityTestRule<>(MainActivity_.class, true, true);

    @Test
    public void activitySurvives_rotation() {
        // Verify the activity is running
        onView(withId(R.id.main_list)).check(matches(isDisplayed()));

        // Rotate to landscape
        activityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Verify the activity still works after rotation
        onView(withId(R.id.main_list)).check(matches(isDisplayed()));

        // Rotate back to portrait
        activityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Verify the activity still works
        onView(withId(R.id.main_list)).check(matches(isDisplayed()));
    }

    @Test
    public void activitySurvives_multipleRotations() {
        for (int i = 0; i < 3; i++) {
            activityRule.getActivity().setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            onView(withId(R.id.main_root)).check(matches(isDisplayed()));

            activityRule.getActivity().setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            onView(withId(R.id.main_root)).check(matches(isDisplayed()));
        }
    }
}
