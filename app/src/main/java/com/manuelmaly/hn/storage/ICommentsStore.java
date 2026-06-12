package com.manuelmaly.hn.storage;

import com.manuelmaly.hn.model.HNPostComments;

/**
 * Persists / restores the last fetched {@link HNPostComments} per post (offline
 * cache). Injectable replacement for the static file logic in
 * {@link com.manuelmaly.hn.util.FileUtil}; methods are synchronous.
 */
public interface ICommentsStore {

    /** Returns the cached comments for the post, or {@code null} if none/unreadable. */
    HNPostComments readLastComments(String postId);

    void writeLastComments(HNPostComments comments, String postId);

}
