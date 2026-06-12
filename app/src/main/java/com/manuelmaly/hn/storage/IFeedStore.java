package com.manuelmaly.hn.storage;

import com.manuelmaly.hn.model.HNFeed;

/**
 * Persists / restores the last fetched {@link HNFeed} (offline cache). Injectable
 * replacement for the static file logic in {@link com.manuelmaly.hn.util.FileUtil};
 * methods are synchronous (threading is the caller's concern).
 */
public interface IFeedStore {

    /** Returns the cached feed, or {@code null} if none/unreadable. */
    HNFeed readLastFeed();

    void writeLastFeed(HNFeed feed);

}
