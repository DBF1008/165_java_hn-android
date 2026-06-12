package com.manuelmaly.hn.testutil;

import com.manuelmaly.hn.data.HNApiException;
import com.manuelmaly.hn.data.IHNRemoteDataSource;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPostComments;

/**
 * Configurable fake remote source. Set the {@code *Result} fields for success or
 * the {@code *Error} fields to throw; call counters and last-argument fields let
 * tests assert what was requested.
 */
public class FakeRemoteDataSource implements IHNRemoteDataSource {

    public HNFeed feedResult;
    public HNApiException feedError;

    public HNFeed moreResult;
    public HNApiException moreError;

    public HNPostComments commentsResult;
    public HNApiException commentsError;

    public boolean voteResult;
    public HNApiException voteError;

    public int fetchFeedCount;
    public int fetchMoreCount;
    public int fetchCommentsCount;
    public int voteCount;

    public HNFeed lastFetchMoreArg;
    public String lastFetchCommentsPostId;
    public String lastVoteUrl;

    @Override
    public HNFeed fetchFeed() throws HNApiException {
        fetchFeedCount++;
        if (feedError != null) {
            throw feedError;
        }
        return feedResult;
    }

    @Override
    public HNFeed fetchMore(HNFeed previous) throws HNApiException {
        fetchMoreCount++;
        lastFetchMoreArg = previous;
        if (moreError != null) {
            throw moreError;
        }
        return moreResult;
    }

    @Override
    public HNPostComments fetchComments(String postId) throws HNApiException {
        fetchCommentsCount++;
        lastFetchCommentsPostId = postId;
        if (commentsError != null) {
            throw commentsError;
        }
        return commentsResult;
    }

    @Override
    public boolean vote(String voteUrl) throws HNApiException {
        voteCount++;
        lastVoteUrl = voteUrl;
        if (voteError != null) {
            throw voteError;
        }
        return voteResult;
    }
}
