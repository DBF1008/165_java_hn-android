package com.manuelmaly.hn.task;

import com.manuelmaly.hn.parser.ICommentsParser;
import com.manuelmaly.hn.parser.IFeedParser;
import com.manuelmaly.hn.server.ApiCommandFactory;
import com.manuelmaly.hn.server.ICredentialsRepository;
import com.manuelmaly.hn.storage.ICommentsStore;
import com.manuelmaly.hn.storage.IFeedStore;
import com.manuelmaly.hn.storage.ISettingsRepository;
import com.manuelmaly.hn.util.IBackgroundExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Default {@link TaskFactory}. Holds the shared collaborators (resolved once by
 * Dagger) and constructs each task with the subset it needs. Lives in the
 * {@code task} package so it can call the tasks' package-private constructors.
 */
@Singleton
public class TaskFactoryImpl implements TaskFactory {

    private final ApiCommandFactory mCommandFactory;
    private final IFeedParser mFeedParser;
    private final ICommentsParser mCommentsParser;
    private final IFeedStore mFeedStore;
    private final ICommentsStore mCommentsStore;
    private final ICredentialsRepository mCredentials;
    private final ISettingsRepository mSettings;
    private final IBackgroundExecutor mBackgroundExecutor;
    private final ITaskResultPublisher mPublisher;

    @Inject
    public TaskFactoryImpl(ApiCommandFactory commandFactory, IFeedParser feedParser, ICommentsParser commentsParser,
        IFeedStore feedStore, ICommentsStore commentsStore, ICredentialsRepository credentials,
        ISettingsRepository settings, IBackgroundExecutor backgroundExecutor, ITaskResultPublisher publisher) {
        mCommandFactory = commandFactory;
        mFeedParser = feedParser;
        mCommentsParser = commentsParser;
        mFeedStore = feedStore;
        mCommentsStore = commentsStore;
        mCredentials = credentials;
        mSettings = settings;
        mBackgroundExecutor = backgroundExecutor;
        mPublisher = publisher;
    }

    @Override
    public HNFeedTaskMainFeed createMainFeed(int taskCode) {
        return new HNFeedTaskMainFeed(taskCode, mCommandFactory, mFeedParser, mFeedStore, mCredentials,
            mBackgroundExecutor, mPublisher);
    }

    @Override
    public HNFeedTaskLoadMore createLoadMore(int taskCode) {
        return new HNFeedTaskLoadMore(taskCode, mCommandFactory, mFeedParser, mFeedStore, mCredentials,
            mBackgroundExecutor, mPublisher);
    }

    @Override
    public HNPostCommentsTask createPostComments(String postId, int taskCode) {
        return new HNPostCommentsTask(postId, taskCode, mCommandFactory, mCommentsParser, mCommentsStore, mCredentials,
            mBackgroundExecutor, mPublisher);
    }

    @Override
    public HNLoginTask createLogin(int taskCode) {
        return new HNLoginTask(taskCode, mCommandFactory, mSettings, mBackgroundExecutor, mPublisher);
    }

    @Override
    public HNVoteTask createVote(int taskCode) {
        return new HNVoteTask(taskCode, mCommandFactory, mCredentials, mBackgroundExecutor, mPublisher);
    }

}
