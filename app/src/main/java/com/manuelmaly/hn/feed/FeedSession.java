package com.manuelmaly.hn.feed;

import com.manuelmaly.hn.model.HNFeed;

/**
 * In-memory state for a single feed category: its loaded posts, scroll position and load flags.
 *
 * <p>Framework-free on purpose. {@link #getScrollState()} is deliberately an opaque {@link Object}:
 * at runtime the Activity stores the {@code Parcelable} returned by {@code ListView.onSaveInstanceState()},
 * while unit tests store simple tokens. Keeping it opaque is what lets this class stay testable without
 * pulling in {@code android.os.Parcelable}.
 */
public class FeedSession {

    private final FeedType mType;
    private HNFeed mFeed;
    private Object mScrollState;
    private boolean mLoading;
    /** True when {@link #mFeed} currently holds cached (offline) data rather than a live load. */
    private boolean mFromCache;
    /** True once a live, non-empty load has succeeded; used to avoid auto-refreshing on revisit. */
    private boolean mEverLoaded;

    public FeedSession(FeedType type) {
        mType = type;
        mFeed = new HNFeed();
    }

    public FeedType getType() {
        return mType;
    }

    public HNFeed getFeed() {
        return mFeed;
    }

    public void setFeed(HNFeed feed) {
        mFeed = feed;
    }

    public Object getScrollState() {
        return mScrollState;
    }

    public void setScrollState(Object scrollState) {
        mScrollState = scrollState;
    }

    public boolean isLoading() {
        return mLoading;
    }

    public void setLoading(boolean loading) {
        mLoading = loading;
    }

    public boolean isFromCache() {
        return mFromCache;
    }

    public void setFromCache(boolean fromCache) {
        mFromCache = fromCache;
    }

    public boolean isEverLoaded() {
        return mEverLoaded;
    }

    public void setEverLoaded(boolean everLoaded) {
        mEverLoaded = everLoaded;
    }

    public boolean isEmpty() {
        return mFeed == null || mFeed.getPosts() == null || mFeed.getPosts().isEmpty();
    }
}
