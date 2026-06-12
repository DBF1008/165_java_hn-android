package com.manuelmaly.hn.util;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatDelegate;

import com.manuelmaly.hn.Settings;

/**
 * Central place for the app's dark-mode policy.
 *
 * <p>The bulk of theming is handled declaratively via {@code values-night/} resource
 * overrides and the {@code Theme.AppCompat.DayNight} parent. This helper covers the two
 * things resources can't: turning the stored preference into an {@link AppCompatDelegate}
 * night mode, and recoloring values that are computed in Java at runtime (the comment
 * text colors that the HTML parser bakes in, and the comment indentation bars).
 */
public final class ThemeHelper {

    /** Stored preference values for {@link Settings#PREF_DARKMODE}. Kept stable / not localized. */
    public static final String VALUE_SYSTEM = "system";
    public static final String VALUE_LIGHT = "light";
    public static final String VALUE_DARK = "dark";

    // The HTML parser emits comment text as a gray rgb(g,g,g) where g runs from 0x00
    // (top-level, most prominent on a light background) up to 0xDD (deeply nested).
    // In dark mode we invert that intent: top-level becomes bright and deeper replies
    // get progressively dimmer, but never so dim as to be unreadable on a dark surface.
    private static final int DARK_COMMENT_BRIGHTEST = 0xEE; // maps from g == 0x00
    private static final int DARK_COMMENT_DIMMEST = 0x88;   // maps from g == 0xDD
    private static final int LIGHT_COMMENT_DEEPEST_GRAY = 0xDD;

    private ThemeHelper() {
    }

    /**
     * Maps a stored preference value to an {@link AppCompatDelegate} night-mode constant.
     * Unknown / null values fall back to following the system.
     */
    public static int nightModeForValue(String value) {
        if (VALUE_LIGHT.equals(value)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        } else if (VALUE_DARK.equals(value)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    /**
     * Applies the user's saved dark-mode preference process-wide. Call this as early as
     * possible (e.g. from {@code Application.onCreate}) so it takes effect before any
     * activity is themed, and again whenever the preference changes.
     */
    public static void apply(Context context) {
        AppCompatDelegate.setDefaultNightMode(nightModeForValue(Settings.getDarkModeValue(context)));
    }

    /** True when the given context is currently configured for night (dark) mode. */
    public static boolean isNightMode(Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Converts a light-mode comment text color (a gray produced by the parser) into a
     * dark-mode-appropriate color: bright for top-level comments, dimmer for deeper ones,
     * never black. Pure function of the input gray's luminance.
     */
    public static int commentColorForNight(int lightColor) {
        int gray = Color.red(lightColor); // parser colors are gray, so any channel works
        if (gray < 0) {
            gray = 0;
        } else if (gray > LIGHT_COMMENT_DEEPEST_GRAY) {
            gray = LIGHT_COMMENT_DEEPEST_GRAY;
        }
        int span = DARK_COMMENT_BRIGHTEST - DARK_COMMENT_DIMMEST;
        int value = DARK_COMMENT_BRIGHTEST - Math.round((gray / (float) LIGHT_COMMENT_DEEPEST_GRAY) * span);
        return Color.rgb(value, value, value);
    }

    /**
     * Color for a single comment-indentation bar at the given nesting level. The bars are
     * subtle dark overlays on a light background; on a dark background they must instead be
     * light overlays to remain visible. Alpha fades with depth, matching the original look.
     */
    public static int commentSpacerColor(int level, boolean night) {
        int alpha = Math.max(70 - level * 10, 10);
        return night ? Color.argb(alpha, 255, 255, 255) : Color.argb(alpha, 0, 0, 0);
    }
}
