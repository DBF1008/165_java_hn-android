package com.manuelmaly.hn.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuelmaly.hn.data.HNFeedRepository;
import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ViewModel for the main feed screen.
 * Holds all feed-related state that survives configuration changes:
 * - Feed data (list of posts)
 * - Read/unread tracking
 * - Upvote state
 * - Loading/error status
 */
public class MainViewModel extends AndroidViewModel {

    private final HNFeedRepository repository;

    /** Combined feed + read/vote state as UI-ready models */
    private final MediatorLiveData<List<PostUiModel>> postList = new MediatorLiveData<>();

    /** Posts that have been upvoted in this session */
    private final MutableLiveData<Set<HNPost>> upvotedPosts =
            new MutableLiveData<>(new HashSet<HNPost>());

    /** Current feed data */
    private final MutableLiveData<HNFeed> currentFeed =
            new MutableLiveData<>(new HNFeed(new ArrayList<HNPost>(), null, ""));

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new HNFeedRepository(application);

        // Merge feed data with read state and upvote state to produce PostUiModel list
        postList.addSource(repository.getFeed(), resource -> rebuildPostList());
        postList.addSource(repository.getAlreadyRead(), alreadyRead -> rebuildPostList());
        postList.addSource(upvotedPosts, voted -> rebuildPostList());
    }

    /**
     * Package-private constructor for testing with a mock repository.
     */
    MainViewModel(@NonNull Application application, HNFeedRepository repository) {
        super(application);
        this.repository = repository;

        postList.addSource(repository.getFeed(), resource -> rebuildPostList());
        postList.addSource(repository.getAlreadyRead(), alreadyRead -> rebuildPostList());
        postList.addSource(upvotedPosts, voted -> rebuildPostList());
    }

    /**
     * Returns the combined post list with UI state. Observe this from the Activity.
     */
    public LiveData<List<PostUiModel>> getPostList() {
        return postList;
    }

    /**
     * Returns the raw feed loading/error status. Observe for refresh indicator and error toasts.
     */
    public LiveData<Resource<HNFeed>> getFeedStatus() {
        return repository.getFeed();
    }

    /**
     * Returns load-more operation results.
     */
    public LiveData<Resource<HNFeed>> getLoadMoreResult() {
        return repository.getLoadMoreResult();
    }

    /**
     * Returns the set of upvoted posts. Survives configuration changes.
     */
    public LiveData<Set<HNPost>> getUpvotedPosts() {
        return upvotedPosts;
    }

    /**
     * Returns the current feed data (for load-more operations that need the existing feed).
     */
    public LiveData<HNFeed> getCurrentFeed() {
        return currentFeed;
    }

    /**
     * Triggers a feed refresh.
     */
    public void refresh() {
        repository.refresh();
    }

    /**
     * Loads cached feed data from disk for immediate display.
     */
    public void loadCachedFeed() {
        repository.loadCachedFeed();
    }

    /**
     * Triggers loading more posts.
     */
    public void loadMore() {
        HNFeed feed = currentFeed.getValue();
        if (feed != null) {
            repository.loadMore(feed);
        }
    }

    /**
     * Marks a post as read. The read state is persisted to SharedPreferences.
     */
    public void markAsRead(HNPost post) {
        repository.markAsRead(post);
    }

    /**
     * Records a successful upvote. Survives configuration changes.
     */
    public void recordUpvote(HNPost post) {
        Set<HNPost> current = upvotedPosts.getValue();
        if (current == null) {
            current = new HashSet<>();
        }
        Set<HNPost> updated = new HashSet<>(current);
        updated.add(post);
        upvotedPosts.setValue(updated);
    }

    /**
     * Updates the current feed reference (called when new feed data arrives).
     */
    public void updateCurrentFeed(HNFeed feed) {
        currentFeed.setValue(feed);
    }

    /**
     * Loads the already-read cache from SharedPreferences.
     */
    public void loadAlreadyReadCache() {
        repository.loadAlreadyReadCache();
    }

    /**
     * Checks if load-more is currently in progress.
     */
    public boolean isLoadMoreRunning() {
        return repository.isLoadMoreRunning();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.dispose();
    }

    private void rebuildPostList() {
        Resource<HNFeed> feedResource = repository.getFeed().getValue();
        Set<Integer> alreadyRead = repository.getAlreadyRead().getValue();
        Set<HNPost> upvoted = upvotedPosts.getValue();

        if (feedResource == null || feedResource.data == null) {
            // Use currentFeed as fallback
            HNFeed feed = currentFeed.getValue();
            if (feed == null || feed.getPosts() == null) {
                postList.setValue(Collections.<PostUiModel>emptyList());
                return;
            }
            List<PostUiModel> models = new ArrayList<>();
            for (HNPost post : feed.getPosts()) {
                boolean read = alreadyRead != null && alreadyRead.contains(post.getTitle().hashCode());
                boolean voted = upvoted != null && upvoted.contains(post);
                models.add(new PostUiModel(post, read, voted));
            }
            postList.setValue(models);
            return;
        }

        HNFeed feed = feedResource.data;
        currentFeed.setValue(feed);

        List<PostUiModel> models = new ArrayList<>();
        if (feed.getPosts() != null) {
            for (HNPost post : feed.getPosts()) {
                boolean read = alreadyRead != null && alreadyRead.contains(post.getTitle().hashCode());
                boolean voted = upvoted != null && upvoted.contains(post);
                models.add(new PostUiModel(post, read, voted));
            }
        }
        postList.setValue(models);
    }
}
