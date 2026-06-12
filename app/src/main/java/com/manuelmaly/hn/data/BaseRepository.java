package com.manuelmaly.hn.data;

import com.manuelmaly.hn.concurrent.AppExecutors;

/**
 * Shared plumbing for the repositories: the data source / cache / executor
 * collaborators, voting (identical for posts and comments), and the helpers that
 * marshal a {@link Resource} back onto the main thread.
 */
public abstract class BaseRepository {

    protected final IHNRemoteDataSource mRemote;
    protected final ILocalCache mCache;
    protected final AppExecutors mExecutors;

    protected BaseRepository(IHNRemoteDataSource remote, ILocalCache cache, AppExecutors executors) {
        mRemote = remote;
        mCache = cache;
        mExecutors = executors;
    }

    public void vote(final String voteUrl, final RepoCallback<Boolean> callback) {
        mExecutors.background(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean accepted = mRemote.vote(voteUrl);
                    deliverSuccess(callback, accepted);
                } catch (HNApiException e) {
                    deliverError(callback, e.getErrorCode(), Boolean.FALSE);
                }
            }
        });
    }

    protected <T> void deliverSuccess(final RepoCallback<T> callback, final T data) {
        mExecutors.main(new Runnable() {
            @Override
            public void run() {
                callback.onResult(Resource.success(data));
            }
        });
    }

    protected <T> void deliverError(final RepoCallback<T> callback, final int errorCode, final T previous) {
        mExecutors.main(new Runnable() {
            @Override
            public void run() {
                callback.onResult(Resource.error(errorCode, previous));
            }
        });
    }
}
