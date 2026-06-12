package com.manuelmaly.hn.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Covers the <b>page-restore</b> scenario: on {@code onResume()} a screen re-reads the font size and
 * should only refresh its views when the value actually changed since it was last applied. This is
 * the behaviour that was inlined as {@code mCurrentFontSize} + {@code refreshFontSizes()}.
 */
public class FontSizeTrackerTest {

    @Test
    public void firstUpdateReportsChangeAndExposesProfile() {
        FontSizeTracker tracker = new FontSizeTracker();
        assertTrue(tracker.update(FontSize.NORMAL));
        assertEquals(FontSize.NORMAL, tracker.current());
        assertEquals(16, tracker.profile().commentText());
    }

    @Test
    public void repeatedSameSizeReportsNoChange() {
        FontSizeTracker tracker = new FontSizeTracker();
        tracker.update(FontSize.NORMAL);
        assertFalse(tracker.update(FontSize.NORMAL));
    }

    @Test
    public void changedSizeReportsChangeAndUpdatesProfile() {
        FontSizeTracker tracker = new FontSizeTracker();
        tracker.update(FontSize.NORMAL);
        assertTrue(tracker.update(FontSize.BIG));
        assertEquals(FontSize.BIG, tracker.current());
        assertEquals(22, tracker.profile().listTitle());
    }

    @Test
    public void returningAfterAChangeRefreshesEachTimeTheValueDiffers() {
        // Open page (NORMAL) -> change to SMALL and return -> return again unchanged -> back to NORMAL.
        FontSizeTracker tracker = new FontSizeTracker();
        tracker.update(FontSize.NORMAL);
        assertTrue(tracker.update(FontSize.SMALL));
        assertFalse(tracker.update(FontSize.SMALL));
        assertTrue(tracker.update(FontSize.NORMAL));
    }
}
