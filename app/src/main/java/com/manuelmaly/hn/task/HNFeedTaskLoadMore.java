package com.manuelmaly.hn.task;

import android.app.Activity;
import android.content.Context;

import com.manuelmaly.hn.feed.FeedType;
import com.manuelmaly.hn.model.HNFeed;

import java.util.EnumMap;
import java.util.Map;

/**
 * Loads the next page of a feed and appends it to an existing {@link HNFeed}. One instance per
 * {@link FeedType} (own broadcast id {@code "HNFeedLoadMore_<id>"}) so paginating one feed never
 * cancels or cross-delivers with another. Does NOT override {@code onResultParsed}, so a partial
 * "load more" page never overwrites the per-feed offline cache.
 */
public class HNFeedTaskLoadMore extends HNFeedTaskBase {

    private static final Map<FeedType, HNFeedTaskLoadMore> sInstances =
            new EnumMap<FeedType, HNFeedTaskLoadMore>(FeedType.class);

    private final FeedType mFeedType;
    private HNFeed mFeedToAttachResultsTo;

    private static HNFeedTaskLoadMore getInstance(FeedType feedType, int taskCode) {
        synchronized (HNFeedTaskLoadMore.class) {
            HNFeedTaskLoadMore task = sInstances.get(feedType);
            if (task == null) {
                task = new HNFeedTaskLoadMore(feedType, taskCode);
                sInstances.put(feedType, task);
            }
            return task;
        }
    }

    private HNFeedTaskLoadMore(FeedType feedType, int taskCode) {
        super("HNFeedLoadMore_" + feedType.getId(), taskCode);
        mFeedType = feedType;
    }

    @Override
    protected String getFeedURL() {
        return mFeedToAttachResultsTo.getNextPageURL();
    }

    public static void start(Activity activity, ITaskFinishedHandler<HNFeed> finishedHandler,
            HNFeed feedToAttachResultsTo, FeedType feedType, int taskCode) {
        HNFeedTaskLoadMore task = getInstance(feedType, taskCode);
        task.setTag(feedType);
        task.setOnFinishedHandler(activity, finishedHandler, HNFeed.class);
        task.setFeedToAttachResultsTo(feedToAttachResultsTo);
        if (task.isRunning()) {
            task.cancel();
        }
        task.startInBackground();
    }

    public static void stopCurrent(Context applicationContext, FeedType feedType) {
        getInstance(feedType, 0).cancel();
    }

    public static boolean isRunning(Context applicationContext, FeedType feedType) {
        return getInstance(feedType, 0).isRunning();
    }

    public void setFeedToAttachResultsTo(HNFeed feedToAttachResultsTo) {
        mFeedToAttachResultsTo = feedToAttachResultsTo;
    }
}
