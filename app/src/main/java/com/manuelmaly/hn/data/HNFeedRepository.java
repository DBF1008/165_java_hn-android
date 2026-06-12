package com.manuelmaly.hn.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.manuelmaly.hn.App;
import com.manuelmaly.hn.Settings;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.task.BaseTask;
import com.manuelmaly.hn.task.HNFeedTaskLoadMore;
import com.manuelmaly.hn.task.HNFeedTaskMainFeed;
import com.manuelmaly.hn.util.FileUtil;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Repository for HN feed data. Bridges the existing BaseTask/LocalBroadcastManager
 * task framework with LiveData for the MVVM layer.
 *
 * Responsibilities:
 * - Triggers feed refresh and load-more via existing task singletons
 * - Listens for task completion broadcasts and posts results to LiveData
 * - Manages the "already read" post tracking via SharedPreferences
 * - Loads cached feed data from disk
 */
public class HNFeedRepository {

    private static final String ALREADY_READ_ARTICLES_KEY = "HN_ALREADY_READ";
    private static final long TWO_DAYS_MS = 2L * 24 * 60 * 60 * 1000;

    private final Context appContext;
    private final MutableLiveData<Resource<HNFeed>> feedLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<HNFeed>> loadMoreLiveData = new MutableLiveData<>();
    private final MutableLiveData<Set<Integer>> alreadyReadLiveData = new MutableLiveData<>(new HashSet<Integer>());

    private BroadcastReceiver feedReceiver;
    private BroadcastReceiver loadMoreReceiver;

    public HNFeedRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Returns LiveData observing the main feed state (loading/success/error).
     */
    public LiveData<Resource<HNFeed>> getFeed() {
        return feedLiveData;
    }

    /**
     * Returns LiveData observing load-more results.
     */
    public LiveData<Resource<HNFeed>> getLoadMoreResult() {
        return loadMoreLiveData;
    }

    /**
     * Returns LiveData observing the set of already-read post title hash codes.
     */
    public LiveData<Set<Integer>> getAlreadyRead() {
        return alreadyReadLiveData;
    }

    /**
     * Triggers a feed refresh by starting the main feed task.
     * Results are posted to {@link #getFeed()}.
     */
    public void refresh() {
        feedLiveData.postValue(Resource.<HNFeed>loading(null));
        unregisterFeedReceiver();

        feedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode = intent.getIntExtra(BaseTask.BROADCAST_INTENT_EXTRA_ERROR,
                        IAPICommand.ERROR_NONE);
                Serializable rawResult = intent.getSerializableExtra(BaseTask.BROADCAST_INTENT_EXTRA_RESULT);

                if (errorCode == IAPICommand.ERROR_NONE && rawResult instanceof HNFeed) {
                    feedLiveData.postValue(Resource.success((HNFeed) rawResult));
                } else {
                    feedLiveData.postValue(Resource.<HNFeed>error(
                            "Failed to load feed (error " + errorCode + ")", null));
                }
            }
        };

        IntentFilter filter = new IntentFilter(HNFeedTaskMainFeed.BROADCAST_INTENT_ID);
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(feedReceiver, filter);

        HNFeedTaskMainFeed task = HNFeedTaskMainFeed.getInstance(0);
        if (!task.isRunning()) {
            task.startInBackground();
        }
    }

    /**
     * Triggers loading more posts (pagination).
     * Results are posted to {@link #getLoadMoreResult()}.
     */
    public void loadMore(HNFeed currentFeed) {
        loadMoreLiveData.postValue(Resource.<HNFeed>loading(null));
        unregisterLoadMoreReceiver();

        loadMoreReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode = intent.getIntExtra(BaseTask.BROADCAST_INTENT_EXTRA_ERROR,
                        IAPICommand.ERROR_NONE);
                Serializable rawResult = intent.getSerializableExtra(BaseTask.BROADCAST_INTENT_EXTRA_RESULT);

                if (errorCode == IAPICommand.ERROR_NONE && rawResult instanceof HNFeed) {
                    loadMoreLiveData.postValue(Resource.success((HNFeed) rawResult));
                } else {
                    loadMoreLiveData.postValue(Resource.<HNFeed>error(
                            "Failed to load more posts", null));
                }
            }
        };

        IntentFilter filter = new IntentFilter(HNFeedTaskLoadMore.BROADCAST_INTENT_ID);
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(loadMoreReceiver, filter);

        HNFeedTaskLoadMore task = HNFeedTaskLoadMore.getInstance(0);
        task.setFeedToAttachResultsTo(currentFeed);
        if (task.isRunning()) {
            task.cancel();
        }
        task.startInBackground();
    }

    /**
     * Checks if the load-more task is currently running.
     */
    public boolean isLoadMoreRunning() {
        return HNFeedTaskLoadMore.isRunning(appContext, 0);
    }

    /**
     * Loads cached feed data from disk (for immediate display before network returns).
     * Only posts to LiveData if the cached data is for the current user.
     */
    public void loadCachedFeed() {
        new FileUtil.GetLastHNFeedTask() {
            @Override
            protected void onPostExecute(HNFeed result) {
                if (result != null
                        && result.getUserAcquiredFor() != null
                        && result.getUserAcquiredFor().equals(
                                Settings.getUserName(App.getInstance()))) {
                    feedLiveData.postValue(Resource.success(result));
                }
            }
        }.execute((Void) null);
    }

    /**
     * Loads the "already read" cache from SharedPreferences, pruning entries older than 2 days.
     * Results are posted to {@link #getAlreadyRead()}.
     */
    public void loadAlreadyReadCache() {
        Set<Integer> alreadyRead = new HashSet<Integer>();
        SharedPreferences sharedPref = appContext.getSharedPreferences(
                ALREADY_READ_ARTICLES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Map<String, ?> read = sharedPref.getAll();
        long now = new Date().getTime();

        for (Map.Entry<String, ?> entry : read.entrySet()) {
            Long readAt = (Long) entry.getValue();
            long diff = (now - readAt) / (24 * 60 * 60 * 1000);
            if (diff >= 2) {
                editor.remove(entry.getKey());
            } else {
                alreadyRead.add(entry.getKey().hashCode());
            }
        }
        editor.apply();
        alreadyReadLiveData.postValue(alreadyRead);
    }

    /**
     * Marks a post as read, persisting to SharedPreferences.
     */
    public void markAsRead(HNPost post) {
        long now = new Date().getTime();
        String title = post.getTitle();
        SharedPreferences.Editor editor = appContext.getSharedPreferences(
                ALREADY_READ_ARTICLES_KEY, Context.MODE_PRIVATE).edit();
        editor.putLong(title, now);
        editor.apply();

        Set<Integer> current = alreadyReadLiveData.getValue();
        if (current == null) {
            current = new HashSet<Integer>();
        }
        Set<Integer> updated = new HashSet<Integer>(current);
        updated.add(title.hashCode());
        alreadyReadLiveData.postValue(updated);
    }

    /**
     * Checks if the given post has been marked as read.
     */
    public boolean isRead(HNPost post) {
        Set<Integer> current = alreadyReadLiveData.getValue();
        return current != null && current.contains(post.getTitle().hashCode());
    }

    /**
     * Cleans up broadcast receivers. Should be called from ViewModel.onCleared().
     */
    public void dispose() {
        unregisterFeedReceiver();
        unregisterLoadMoreReceiver();
    }

    private void unregisterFeedReceiver() {
        if (feedReceiver != null) {
            LocalBroadcastManager.getInstance(App.getInstance()).unregisterReceiver(feedReceiver);
            feedReceiver = null;
        }
    }

    private void unregisterLoadMoreReceiver() {
        if (loadMoreReceiver != null) {
            LocalBroadcastManager.getInstance(App.getInstance()).unregisterReceiver(loadMoreReceiver);
            loadMoreReceiver = null;
        }
    }
}
