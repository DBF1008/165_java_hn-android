package com.manuelmaly.hn.data.storage;

import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPostComments;

/**
 * Abstraction over file-based serialization cache for testability.
 * Wraps FileUtil's getLastHNFeed/setLastHNFeed and comment cache methods.
 */
public interface FeedCache {

    HNFeed getLastFeed();

    void setLastFeed(HNFeed feed);

    HNPostComments getLastComments(String postId);

    void setLastComments(String postId, HNPostComments comments);
}
