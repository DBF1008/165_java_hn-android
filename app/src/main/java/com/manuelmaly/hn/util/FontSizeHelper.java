package com.manuelmaly.hn.util;

import android.content.Context;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.Settings;

/**
 * Centralized font size resolution from user preferences.
 * Replaces duplicate refreshFontSizes() logic previously embedded in
 * MainActivity and CommentsActivity.
 */
public final class FontSizeHelper {

    public static class FeedFontSizes {
        public final int titleSize;
        public final int detailsSize;

        public FeedFontSizes(int titleSize, int detailsSize) {
            this.titleSize = titleSize;
            this.detailsSize = detailsSize;
        }
    }

    public static class CommentFontSizes {
        public final int textSize;
        public final int metadataSize;

        public CommentFontSizes(int textSize, int metadataSize) {
            this.textSize = textSize;
            this.metadataSize = metadataSize;
        }
    }

    private FontSizeHelper() {}

    /**
     * Resolves the user's font size preference to concrete pixel values for the feed list.
     */
    public static FeedFontSizes getFeedFontSizes(Context context) {
        String fontSize = Settings.getFontSize(context);
        if (fontSize.equals(context.getString(R.string.pref_fontsize_small))) {
            return new FeedFontSizes(15, 11);
        } else if (fontSize.equals(context.getString(R.string.pref_fontsize_normal))) {
            return new FeedFontSizes(18, 12);
        } else {
            return new FeedFontSizes(22, 15);
        }
    }

    /**
     * Resolves the user's font size preference to concrete pixel values for the comments list.
     */
    public static CommentFontSizes getCommentFontSizes(Context context) {
        String fontSize = Settings.getFontSize(context);
        if (fontSize.equals(context.getString(R.string.pref_fontsize_small))) {
            return new CommentFontSizes(14, 12);
        } else if (fontSize.equals(context.getString(R.string.pref_fontsize_normal))) {
            return new CommentFontSizes(16, 14);
        } else {
            return new CommentFontSizes(20, 18);
        }
    }

    /**
     * Returns the raw font size preference string, useful for change detection.
     */
    public static String getCurrentFontSizePreference(Context context) {
        return Settings.getFontSize(context);
    }
}
