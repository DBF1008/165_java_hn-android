package com.manuelmaly.hn.data;

import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPostComments;

/**
 * Remote data seam for Hacker News. Production is {@link HNRemoteDataSource} (HTTP
 * command + HTML parser); unit tests inject a fake. Methods are synchronous and
 * meant to be invoked on a background thread by the repository layer.
 *
 * <p>Replaces the Activity-bound task singletons (HNFeedTaskMainFeed,
 * HNFeedTaskLoadMore, HNPostCommentsTask, HNVoteTask): same underlying
 * command+parser work, but with no Activity/LocalBroadcastManager coupling.</p>
 */
public interface IHNRemoteDataSource {

    /** Loads the main feed. */
    HNFeed fetchFeed() throws HNApiException;

    /** Loads the next page, using {@code previous.getNextPageURL()}. */
    HNFeed fetchMore(HNFeed previous) throws HNApiException;

    /** Loads the comment tree for a post. */
    HNPostComments fetchComments(String postId) throws HNApiException;

    /**
     * Casts a vote at the given (auth-bearing) URL. Returns {@code true} on a
     * successful, accepted vote; {@code false} if the server rejected it (e.g. not
     * logged in). Throws only on network/communication failure.
     */
    boolean vote(String voteUrl) throws HNApiException;
}
