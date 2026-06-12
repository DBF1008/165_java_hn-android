package com.manuelmaly.hn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Verifies the dark theme is actually wired for each in-app page: the colors that drive
 * the article list, comments, and article-reader surfaces must resolve to different
 * (darker) values under the night configuration. This is the resource-level guarantee
 * behind both the system switch and the manual Dark switch.
 */
@RunWith(RobolectricTestRunner.class)
public class NightResourcesTest {

    /** Resolves a color resource under an explicit night / not-night configuration. */
    private int colorFor(int resId, boolean night) {
        RuntimeEnvironment.setQualifiers(night ? "night" : "notnight");
        return RuntimeEnvironment.getApplication().getResources().getColor(resId);
    }

    private double luminance(int c) {
        return 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c);
    }

    @Test
    public void everyThemedColorHasANightOverride() {
        int[] themedColors = {
                // Surfaces (article list + comments backgrounds, list-row selector)
                R.color.peach, R.color.peach_dark,
                // Article list
                R.color.list_divider, R.color.post_meta, R.color.list_item_alt_bg,
                R.color.dark_gray_post_title, R.color.gray_post_title_read,
                R.color.loadmore_bg, R.color.loadmore_text, R.color.loading_text,
                // Comments
                R.color.gray_comments_information, R.color.gray_comments_divider,
                R.color.comment_text_default,
                // Article reader
                R.color.gray_article_title, R.color.webview_bg,
                // Settings
                R.color.settings_text, R.color.settings_summary,
        };
        for (int id : themedColors) {
            assertNotEquals("expected a values-night override for resource id " + id,
                    colorFor(id, false), colorFor(id, true));
        }
    }

    @Test
    public void surfaceIsDarkerInNight() {
        assertTrue("the peach surface must be darker in night mode",
                luminance(colorFor(R.color.peach, true)) < luminance(colorFor(R.color.peach, false)));
    }

    @Test
    public void postTitleFlipsFromDarkToLight() {
        assertTrue("post title is dark text in light mode",
                luminance(colorFor(R.color.dark_gray_post_title, false)) < 128);
        assertTrue("post title is light text in dark mode",
                luminance(colorFor(R.color.dark_gray_post_title, true)) > 128);
    }

    @Test
    public void brandOrangeIsNotOverridden() {
        // The orange action bar is intentionally kept in both modes.
        assertEquals(colorFor(R.color.red_dark, false), colorFor(R.color.red_dark, true));
    }
}
