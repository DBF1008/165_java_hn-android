package com.manuelmaly.hn.data;

import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPostComments;
import com.manuelmaly.hn.util.FileUtil;

/**
 * Production {@link ILocalCache}, delegating to the existing {@link FileUtil}
 * serialization cache. Reads use the synchronous blocking accessors; writes use
 * FileUtil's existing fire-and-forget background writers.
 */
public class FileLocalCache implements ILocalCache {

    @Override
    public HNFeed getLastFeed() {
        return FileUtil.getLastHNFeedBlocking();
    }

    @Override
    public void saveFeed(HNFeed feed) {
        FileUtil.setLastHNFeed(feed);
    }

    @Override
    public HNPostComments getLastComments(String postId) {
        return FileUtil.getLastHNPostCommentsBlocking(postId);
    }

    @Override
    public void saveComments(HNPostComments comments, String postId) {
        FileUtil.setLastHNPostComments(comments, postId);
    }
}
