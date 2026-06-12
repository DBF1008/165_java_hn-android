package com.manuelmaly.hn.viewmodel;

import com.manuelmaly.hn.model.HNPost;

/**
 * UI-ready model for a post item in the main feed list.
 * Combines the post data with UI state (read status, vote status)
 * so that the adapter doesn't need to query external state during binding.
 */
public class PostUiModel {

    public final HNPost post;
    public final boolean isRead;
    public final boolean isUpvoted;

    public PostUiModel(HNPost post, boolean isRead, boolean isUpvoted) {
        this.post = post;
        this.isRead = isRead;
        this.isUpvoted = isUpvoted;
    }
}
