package com.manuelmaly.hn.prefs;

/**
 * Tracks the font size last applied to a screen so the screen only refreshes when the size actually
 * changes. This replaces the per-Activity {@code mCurrentFontSize} string plus the change-detecting
 * {@code refreshFontSizes()} that was duplicated in the list and comments screens.
 *
 * <p>Pure (no Android dependencies); the Activity calls {@link #update(FontSize)} in {@code
 * onResume()} and re-applies sizes from {@link #profile()} only when it returns {@code true}.
 */
public final class FontSizeTracker {

    private FontSize current;

    /**
     * Records {@code size} as the latest value and reports whether a refresh is needed: {@code true}
     * on the first call (nothing applied yet) and whenever the size differs from the last applied
     * one, {@code false} when it is unchanged.
     */
    public boolean update(FontSize size) {
        if (current == null || current != size) {
            current = size;
            return true;
        }
        return false;
    }

    /** The last value passed to {@link #update(FontSize)}, or {@code null} if never called. */
    public FontSize current() {
        return current;
    }

    /** The {@link FontSizeProfile} for the current size. Only valid after {@link #update}. */
    public FontSizeProfile profile() {
        return FontSizeProfile.of(current);
    }
}
