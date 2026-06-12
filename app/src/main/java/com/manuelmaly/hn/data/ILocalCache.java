package com.manuelmaly.hn.data;

import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPostComments;

/**
 * Local (on-disk) cache seam. Production is {@link FileLocalCache} (wraps
 * {@code FileUtil}); unit tests inject a fake so the cache path never touches
 * {@code App.getInstance().getFilesDir()} on the JVM.
 *
 * <p>Reads are synchronous (call from a background thread); writes are
 * fire-and-forget.</p>
 */
public interface ILocalCache {

    /** Last cached main feed, or null. */
    HNFeed getLastFeed();

    void saveFeed(HNFeed feed);

    /** Last cached comments for {@code postId}, or null. */
    HNPostComments getLastComments(String postId);

    void saveComments(HNPostComments comments, String postId);
}
