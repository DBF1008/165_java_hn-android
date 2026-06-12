package com.manuelmaly.hn.data;

import com.manuelmaly.hn.concurrent.AppExecutors;
import com.manuelmaly.hn.model.HNPostComments;

/**
 * Orchestrates comment data, mirroring {@link FeedRepository}: fetch + parse on a
 * background thread, cache successful loads, and report a {@link Resource} on the
 * main thread. Replaces the Activity-bound HNPostCommentsTask singleton; voting is
 * inherited from {@link BaseRepository}.
 */
public class CommentsRepository extends BaseRepository {

    public CommentsRepository(IHNRemoteDataSource remote, ILocalCache cache, AppExecutors executors) {
        super(remote, cache, executors);
    }

    /** Emits LOADING(previous), then SUCCESS(comments) or ERROR(code, previous). */
    public void loadComments(final String postId, final HNPostComments previous,
            final RepoCallback<HNPostComments> callback) {
        callback.onResult(Resource.loading(previous));
        mExecutors.background(new Runnable() {
            @Override
            public void run() {
                try {
                    HNPostComments comments = mRemote.fetchComments(postId);
                    mCache.saveComments(comments, postId);
                    deliverSuccess(callback, comments);
                } catch (HNApiException e) {
                    deliverError(callback, e.getErrorCode(), previous);
                }
            }
        });
    }

    /** Loads cached comments for the post (may be null) and delivers them as SUCCESS. */
    public void loadCachedComments(final String postId, final RepoCallback<HNPostComments> callback) {
        mExecutors.background(new Runnable() {
            @Override
            public void run() {
                HNPostComments cached = mCache.getLastComments(postId);
                deliverSuccess(callback, cached);
            }
        });
    }
}
