package com.manuelmaly.hn.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.concurrent.AppExecutors;
import com.manuelmaly.hn.data.FeedRepository;
import com.manuelmaly.hn.data.IReadStateStore;
import com.manuelmaly.hn.data.RepoCallback;
import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds all feed-screen state and the data-processing wiring that used to live in
 * {@code MainActivity}. The Activity becomes a thin view that observes this and
 * forwards user actions.
 *
 * <p>Data that should survive (and replay after) a configuration change is exposed
 * as {@link LiveData} ({@link #getFeed()} etc.); one-shot effects (toasts) use
 * {@link SingleLiveEvent} so they do not re-fire when an observer re-attaches.
 * Login/URL decisions remain in the Activity, so this class needs no
 * {@code Context}/{@code Settings} and is unit-testable on the JVM.</p>
 */
public class MainViewModel extends ViewModel {

    private final FeedRepository mRepository;
    private final IReadStateStore mReadStateStore;
    private final AppExecutors mExecutors;

    private final MutableLiveData<Resource<HNFeed>> mFeed = new MutableLiveData<Resource<HNFeed>>();
    private final MutableLiveData<Boolean> mRefreshing = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> mLoadMoreInFlight = new MutableLiveData<Boolean>();
    private final SingleLiveEvent<Boolean> mVoteEvent = new SingleLiveEvent<Boolean>();
    private final SingleLiveEvent<Integer> mErrorMessage = new SingleLiveEvent<Integer>();

    /** Posts the user has upvoted this session (dedup by {@link HNPost#equals}). */
    private final Set<HNPost> mUpvotedPosts = new HashSet<HNPost>();
    /** {@code title.hashCode()} of read articles; mutated only on the main thread. */
    private final Set<Integer> mAlreadyRead = new HashSet<Integer>();

    public MainViewModel(FeedRepository repository, IReadStateStore readStateStore, AppExecutors executors) {
        mRepository = repository;
        mReadStateStore = readStateStore;
        mExecutors = executors;

        mFeed.setValue(Resource.success(new HNFeed(new ArrayList<HNPost>(), null, "")));
        mRefreshing.setValue(false);
        mLoadMoreInFlight.setValue(false);
    }

    // --- Observable state -------------------------------------------------

    public LiveData<Resource<HNFeed>> getFeed() {
        return mFeed;
    }

    public LiveData<Boolean> getRefreshing() {
        return mRefreshing;
    }

    public LiveData<Boolean> getLoadMoreInFlight() {
        return mLoadMoreInFlight;
    }

    /** Fires {@code true} on a successful vote, {@code false} otherwise. */
    public SingleLiveEvent<Boolean> getVoteEvent() {
        return mVoteEvent;
    }

    /** Fires a string resource id to toast. */
    public SingleLiveEvent<Integer> getErrorMessage() {
        return mErrorMessage;
    }

    // --- Actions ----------------------------------------------------------

    /** Resets to an empty feed (used when the logged-in user changes). */
    public void resetFeed() {
        mFeed.setValue(Resource.success(new HNFeed(new ArrayList<HNPost>(), null, "")));
    }

    public void loadFeed() {
        mRepository.loadFeed(currentFeed(), new RepoCallback<HNFeed>() {
            @Override
            public void onResult(Resource<HNFeed> resource) {
                mFeed.setValue(resource);
                mRefreshing.setValue(resource.isLoading());
                if (resource.isError()) {
                    mErrorMessage.setValue(R.string.error_unable_to_retrieve_feed);
                }
            }
        });
    }

    public void loadMore() {
        if (Boolean.TRUE.equals(mLoadMoreInFlight.getValue())) {
            return;
        }
        final HNFeed current = currentFeed();
        mLoadMoreInFlight.setValue(true);
        mRefreshing.setValue(true);
        mRepository.loadMore(current, new RepoCallback<HNFeed>() {
            @Override
            public void onResult(Resource<HNFeed> resource) {
                mLoadMoreInFlight.setValue(false);
                mRefreshing.setValue(false);

                HNFeed more = resource.getData();
                boolean reachedEnd = !resource.isSuccess() || more == null
                        || more.getPosts() == null || more.getPosts().isEmpty();

                if (current != null) {
                    if (reachedEnd) {
                        current.setLoadedMore(true);
                    } else {
                        current.appendLoadMoreFeed(more);
                    }
                }
                mFeed.setValue(Resource.success(current));

                if (reachedEnd) {
                    mErrorMessage.setValue(R.string.error_unable_to_load_more);
                }
            }
        });
    }

    public void vote(String voteUrl, final HNPost post) {
        mRepository.vote(voteUrl, new RepoCallback<Boolean>() {
            @Override
            public void onResult(Resource<Boolean> resource) {
                boolean accepted = resource.isSuccess() && Boolean.TRUE.equals(resource.getData());
                if (accepted && post != null) {
                    mUpvotedPosts.add(post);
                }
                mVoteEvent.setValue(accepted);
            }
        });
    }

    public void markRead(final HNPost post) {
        if (post == null || post.getTitle() == null) {
            return;
        }
        // Update the in-memory set immediately on the main thread so isRead() is
        // correct for the next bind; persist off-thread.
        mAlreadyRead.add(post.getTitle().hashCode());
        final String title = post.getTitle();
        mExecutors.background(new Runnable() {
            @Override
            public void run() {
                mReadStateStore.markRead(title);
            }
        });
    }

    public void loadAlreadyRead() {
        mExecutors.background(new Runnable() {
            @Override
            public void run() {
                final Set<Integer> hashes = mReadStateStore.loadReadTitleHashes();
                mExecutors.main(new Runnable() {
                    @Override
                    public void run() {
                        mAlreadyRead.addAll(hashes);
                    }
                });
            }
        });
    }

    /** Loads the cached feed as an intermediate result, gated to the current user. */
    public void loadCached(final String currentUserName) {
        mRepository.loadCached(new RepoCallback<HNFeed>() {
            @Override
            public void onResult(Resource<HNFeed> resource) {
                HNFeed cached = resource.getData();
                if (cached == null || cached.getUserAcquiredFor() == null
                        || !cached.getUserAcquiredFor().equals(currentUserName)) {
                    return;
                }
                // Only use the cache if the live feed hasn't populated yet.
                HNFeed current = currentFeed();
                if (current == null || current.getPosts() == null || current.getPosts().isEmpty()) {
                    mFeed.setValue(Resource.success(cached));
                }
            }
        });
    }

    // --- Helpers for the view ---------------------------------------------

    /** The feed currently held (never null after construction). */
    public HNFeed currentFeed() {
        Resource<HNFeed> resource = mFeed.getValue();
        return resource != null ? resource.getData() : null;
    }

    public boolean isRead(HNPost post) {
        return post != null && post.getTitle() != null
                && mAlreadyRead.contains(post.getTitle().hashCode());
    }

    public boolean isUpvoted(HNPost post) {
        return mUpvotedPosts.contains(post);
    }
}
