package com.manuelmaly.hn.task;

/**
 * Builds task instances with their injected collaborators. The static task
 * facades (e.g. {@code HNFeedTaskMainFeed.startOrReattach}) obtain a
 * {@code TaskFactory} from the application's Dagger component instead of
 * {@code new}-ing tasks directly, so the wiring stays in one place and tests can
 * construct tasks with fakes.
 */
public interface TaskFactory {

    HNFeedTaskMainFeed createMainFeed(int taskCode);

    HNFeedTaskLoadMore createLoadMore(int taskCode);

    HNPostCommentsTask createPostComments(String postId, int taskCode);

    HNLoginTask createLogin(int taskCode);

    HNVoteTask createVote(int taskCode);

}
