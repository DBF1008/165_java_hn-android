package com.manuelmaly.hn.data;

import com.manuelmaly.hn.concurrent.AppExecutors;
import com.manuelmaly.hn.model.HNFeed;

/**
 * Orchestrates feed data: runs the (blocking) {@link IHNRemoteDataSource} /
 * {@link ILocalCache} calls on {@link AppExecutors#background(Runnable)} and
 * delivers {@link Resource}s back via {@link AppExecutors#main(Runnable)}.
 *
 * <p>The ViewModel owns user-facing state (in-flight flags, error messages); this
 * class only fetches and reports. Replaces the Activity-bound HNFeedTask* +
 * HNVoteTask singletons ({@link BaseRepository#vote} supplies voting).</p>
 */
public class FeedRepository extends BaseRepository {

    public FeedRepository(IHNRemoteDataSource remote, ILocalCache cache, AppExecutors executors) {
        super(remote, cache, executors);
    }

    /** Emits LOADING(previous) immediately, then SUCCESS(feed) or ERROR(code, previous). */
    public void loadFeed(final HNFeed previous, final RepoCallback<HNFeed> callback) {
        callback.onResult(Resource.loading(previous));
        mExecutors.background(new Runnable() {
            @Override
            public void run() {
                try {
                    HNFeed feed = mRemote.fetchFeed();
                    mCache.saveFeed(feed);
                    deliverSuccess(callback, feed);
                } catch (HNApiException e) {
                    deliverError(callback, e.getErrorCode(), previous);
                }
            }
        });
    }

    /**
     * Fetches the next page. The in-flight flag is managed by the caller, so no
     * LOADING is emitted; result is SUCCESS(nextPage) or ERROR(code, null).
     */
    public void loadMore(final HNFeed previous, final RepoCallback<HNFeed> callback) {
        mExecutors.background(new Runnable() {
            @Override
            public void run() {
                try {
                    HNFeed more = mRemote.fetchMore(previous);
                    deliverSuccess(callback, more);
                } catch (HNApiException e) {
                    deliverError(callback, e.getErrorCode(), null);
                }
            }
        });
    }

    /** Loads the cached feed (may be null) and delivers it as SUCCESS. */
    public void loadCached(final RepoCallback<HNFeed> callback) {
        mExecutors.background(new Runnable() {
            @Override
            public void run() {
                HNFeed cached = mCache.getLastFeed();
                deliverSuccess(callback, cached);
            }
        });
    }
}
