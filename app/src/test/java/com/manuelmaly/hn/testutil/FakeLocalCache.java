package com.manuelmaly.hn.testutil;

import com.manuelmaly.hn.data.ILocalCache;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPostComments;

/** In-memory {@link ILocalCache} that records what was saved. */
public class FakeLocalCache implements ILocalCache {

    public HNFeed cachedFeed;
    public HNPostComments cachedComments;

    public HNFeed savedFeed;
    public HNPostComments savedComments;
    public String savedCommentsPostId;

    @Override
    public HNFeed getLastFeed() {
        return cachedFeed;
    }

    @Override
    public void saveFeed(HNFeed feed) {
        savedFeed = feed;
    }

    @Override
    public HNPostComments getLastComments(String postId) {
        return cachedComments;
    }

    @Override
    public void saveComments(HNPostComments comments, String postId) {
        savedComments = comments;
        savedCommentsPostId = postId;
    }
}
