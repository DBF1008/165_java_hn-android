package com.manuelmaly.hn.data;

import java.io.Serializable;

/**
 * Wraps the result of a vote operation (upvote/downvote on a post or comment).
 */
public class VoteResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public final boolean success;
    public final transient Object votedItem;

    public VoteResult(boolean success, Object votedItem) {
        this.success = success;
        this.votedItem = votedItem;
    }
}
