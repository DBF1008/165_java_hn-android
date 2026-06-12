package com.manuelmaly.hn.feed;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The selectable Hacker News feed categories.
 *
 * <p>This type is intentionally free of any {@code android.*} dependency so that the feed
 * navigation logic (see {@link FeedNavigationController}) can be unit tested on a plain JVM.
 *
 * <p>Each value maps to:
 * <ul>
 *     <li>{@code id} – a stable identifier used for persistence and per-feed cache file names,</li>
 *     <li>{@code path} – the path appended to the HN base URL to fetch the feed,</li>
 *     <li>{@code title} – the label shown in the feed tab strip.</li>
 * </ul>
 *
 * Note: {@link #TOP} maps to {@code /news}, the canonical top-stories page, which returns the
 * same content and markup as the bare {@code https://news.ycombinator.com/} the app used before.
 */
public enum FeedType {

    TOP("top", "news", "Top"),
    NEW("new", "newest", "New"),
    ASK("ask", "ask", "Ask"),
    SHOW("show", "show", "Show"),
    JOBS("jobs", "jobs", "Jobs");

    private static final String HN_BASE_URL = "https://news.ycombinator.com/";

    private final String mId;
    private final String mPath;
    private final String mTitle;

    FeedType(String id, String path, String title) {
        mId = id;
        mPath = path;
        mTitle = title;
    }

    public String getId() {
        return mId;
    }

    public String getPath() {
        return mPath;
    }

    public String getTitle() {
        return mTitle;
    }

    /** Absolute URL of the feed's first page, e.g. {@code https://news.ycombinator.com/newest}. */
    public String getFeedURL() {
        return HN_BASE_URL + mPath;
    }

    /** Per-feed offline cache file name, e.g. {@code lastHNFeed_jobs}. */
    public String cacheFileName() {
        return "lastHNFeed_" + mId;
    }

    /** The feed shown by default (and on first launch). */
    public static FeedType getDefault() {
        return TOP;
    }

    /** Resolves a persisted id back to a {@link FeedType}, falling back when unknown/null. */
    public static FeedType fromId(String id, FeedType fallback) {
        if (id != null) {
            for (FeedType type : values()) {
                if (type.mId.equals(id)) {
                    return type;
                }
            }
        }
        return fallback;
    }

    /** Feed categories in the order they should appear in the tab strip. */
    public static List<FeedType> ordered() {
        return Collections.unmodifiableList(Arrays.asList(values()));
    }
}
