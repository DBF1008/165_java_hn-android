package com.manuelmaly.hn.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.manuelmaly.hn.ArticleReaderActivity;
import com.manuelmaly.hn.ArticleReaderActivity_;
import com.manuelmaly.hn.CommentsActivity;
import com.manuelmaly.hn.R;
import com.manuelmaly.hn.Settings;
import com.manuelmaly.hn.model.HNPost;

/**
 * Centralized navigation helpers extracted from MainActivity's static utility methods.
 * Provides a single place for all cross-Activity navigation logic.
 */
public final class NavigationHelper {

    private NavigationHelper() {}

    /**
     * Opens the given URL in the device's default browser.
     */
    public static void openURLInBrowser(String url, Activity activity) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        activity.startActivity(browserIntent);
    }

    /**
     * Opens the given post in the in-app article reader.
     *
     * @param post                  the HN post to display
     * @param overrideHtmlProvider  optional HTML provider override (e.g., "instapaper"), or null
     * @param activity              the calling Activity
     */
    public static void openPostInApp(HNPost post, String overrideHtmlProvider, Activity activity) {
        Intent i = new Intent(activity, ArticleReaderActivity_.class);
        i.putExtra(ArticleReaderActivity.EXTRA_HNPOST, post);
        if (overrideHtmlProvider != null) {
            i.putExtra(ArticleReaderActivity.EXTRA_HTMLPROVIDER_OVERRIDE, overrideHtmlProvider);
        }
        activity.startActivity(i);
    }

    /**
     * Launches the system share sheet for the given post.
     */
    public static void shareUrl(HNPost post, Activity activity) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, post.getURL());
        activity.startActivity(Intent.createChooser(shareIntent,
                activity.getString(R.string.share_article_url)));
    }

    /**
     * Navigates to the comments screen for the given post.
     */
    public static void openComments(HNPost post, Activity activity) {
        Intent i = new Intent(activity, com.manuelmaly.hn.CommentsActivity_.class);
        i.putExtra(CommentsActivity.EXTRA_HNPOST, post);
        activity.startActivity(i);
    }

    /**
     * Resolves the article view URL for the given post using the user's preferred HTML provider.
     */
    public static String getArticleViewURL(HNPost post, Activity activity) {
        return ArticleReaderActivity.getArticleViewURL(post,
                Settings.getHtmlProvider(activity), activity);
    }

    /**
     * Opens the given post according to the user's preferences:
     * - If the post is from news.ycombinator.com, opens comments
     * - If the user prefers the browser, opens in browser
     * - Otherwise, opens in the in-app reader
     */
    public static void openPost(HNPost post, Activity activity) {
        if (post.getURLDomain().equals("news.ycombinator.com")) {
            openComments(post, activity);
        } else if (Settings.getHtmlViewer(activity).equals(
                activity.getString(R.string.pref_htmlviewer_browser))) {
            openURLInBrowser(getArticleViewURL(post, activity), activity);
        } else {
            openPostInApp(post, null, activity);
        }
    }
}
