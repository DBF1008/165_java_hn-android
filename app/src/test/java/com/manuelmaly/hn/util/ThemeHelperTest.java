package com.manuelmaly.hn.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ApplicationProvider;

import com.manuelmaly.hn.Settings;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Regression coverage for the dark-mode policy: the manual System/Light/Dark switch
 * (preference -> AppCompatDelegate mode), night-mode detection (used by the system
 * switch path), and the runtime recoloring of comment text / indentation bars.
 */
@RunWith(RobolectricTestRunner.class)
public class ThemeHelperTest {

    @After
    public void resetNightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    // ---- Manual switch: stored preference value -> night mode ----

    @Test
    public void nightModeForValue_light_isNo() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO,
                ThemeHelper.nightModeForValue(ThemeHelper.VALUE_LIGHT));
    }

    @Test
    public void nightModeForValue_dark_isYes() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES,
                ThemeHelper.nightModeForValue(ThemeHelper.VALUE_DARK));
    }

    @Test
    public void nightModeForValue_system_followsSystem() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                ThemeHelper.nightModeForValue(ThemeHelper.VALUE_SYSTEM));
    }

    @Test
    public void nightModeForValue_unknownOrNull_followsSystem() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                ThemeHelper.nightModeForValue("bogus"));
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                ThemeHelper.nightModeForValue(null));
    }

    // ---- Persistence + apply(): saved preference drives the delegate ----

    @Test
    public void getDarkModeValue_defaultsToSystem() {
        Context c = ApplicationProvider.getApplicationContext();
        assertEquals(ThemeHelper.VALUE_SYSTEM, Settings.getDarkModeValue(c));
    }

    @Test
    public void apply_appliesPersistedPreference() {
        Context c = ApplicationProvider.getApplicationContext();

        PreferenceManager.getDefaultSharedPreferences(c).edit()
                .putString(Settings.PREF_DARKMODE, ThemeHelper.VALUE_DARK).commit();
        ThemeHelper.apply(c);
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode());

        PreferenceManager.getDefaultSharedPreferences(c).edit()
                .putString(Settings.PREF_DARKMODE, ThemeHelper.VALUE_LIGHT).commit();
        ThemeHelper.apply(c);
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode());
    }

    // ---- System switch detection ----

    @Test
    public void isNightMode_falseByDefault() {
        assertFalse(ThemeHelper.isNightMode(ApplicationProvider.getApplicationContext()));
    }

    @Test
    @Config(qualifiers = "night")
    public void isNightMode_trueUnderNightConfiguration() {
        assertTrue(ThemeHelper.isNightMode(ApplicationProvider.getApplicationContext()));
    }

    // ---- Comment text recolor: legible at every nesting depth in dark mode ----

    @Test
    public void commentColorForNight_topLevelBlack_becomesBright() {
        int night = ThemeHelper.commentColorForNight(Color.rgb(0, 0, 0));
        assertTrue("top-level comment should be bright in dark mode", Color.red(night) >= 0xCC);
    }

    @Test
    public void commentColorForNight_neverNearBlack() {
        for (int g = 0; g <= 0xDD; g += 0x11) {
            int night = ThemeHelper.commentColorForNight(Color.rgb(g, g, g));
            assertTrue("dark-mode comment text must stay readable (>= 0x80) for gray " + g,
                    Color.red(night) >= 0x80);
        }
    }

    @Test
    public void commentColorForNight_deeperIsDimmerThanTopLevel() {
        int top = ThemeHelper.commentColorForNight(Color.rgb(0, 0, 0));
        int deep = ThemeHelper.commentColorForNight(Color.rgb(0xDD, 0xDD, 0xDD));
        assertTrue("deeper replies should be dimmer than top-level", Color.red(deep) < Color.red(top));
    }

    // ---- Indentation bars: visible overlay color in both modes ----

    @Test
    public void commentSpacerColor_nightUsesWhiteChannel() {
        int night = ThemeHelper.commentSpacerColor(0, true);
        assertEquals(255, Color.red(night));
        assertEquals(255, Color.green(night));
        assertEquals(255, Color.blue(night));
        assertTrue(Color.alpha(night) > 0);
    }

    @Test
    public void commentSpacerColor_dayUsesBlackChannel() {
        int day = ThemeHelper.commentSpacerColor(0, false);
        assertEquals(0, Color.red(day));
        assertEquals(0, Color.green(day));
        assertEquals(0, Color.blue(day));
    }

    @Test
    public void commentSpacerColor_alphaFadesWithDepthToFloor() {
        assertEquals(70, Color.alpha(ThemeHelper.commentSpacerColor(0, false)));
        assertEquals(60, Color.alpha(ThemeHelper.commentSpacerColor(1, false)));
        assertEquals(10, Color.alpha(ThemeHelper.commentSpacerColor(10, false))); // clamped floor
    }
}
