package com.manuelmaly.hn.task;

import android.app.Activity;
import android.content.Context;

import com.manuelmaly.hn.feed.FeedType;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.util.FileUtil;

import java.util.EnumMap;
import java.util.Map;

/**
 * Loads the first page of a single feed category (Top / New / Ask / Show / Jobs).
 *
 * <p>Replaces the old single-feed {@code HNFeedTaskMainFeed}. There is one instance per
 * {@link FeedType}, each with its own broadcast id ({@code "HNFeed_<id>"}), so different feeds can be
 * refreshed concurrently without their finished-notifications crossing. The result is tagged with the
 * {@link FeedType} (delivered as the {@code tag} of
 * {@link ITaskFinishedHandler#onTaskFinished}) so the Activity can route it to the right feed.
 */
public class HNFeedTask extends HNFeedTaskBase {

    private static final Map<FeedType, HNFeedTask> sInstances =
            new EnumMap<FeedType, HNFeedTask>(FeedType.class);

    private final FeedType mFeedType;

    private static HNFeedTask getInstance(FeedType feedType, int taskCode) {
        synchronized (HNFeedTaskBase.class) {
            HNFeedTask task = sInstances.get(feedType);
            if (task == null) {
                task = new HNFeedTask(feedType, taskCode);
                sInstances.put(feedType, task);
            }
            return task;
        }
    }

    private HNFeedTask(FeedType feedType, int taskCode) {
        super("HNFeed_" + feedType.getId(), taskCode);
        mFeedType = feedType;
    }

    @Override
    protected String getFeedURL() {
        return mFeedType.getFeedURL();
    }

    @Override
    protected void onResultParsed(HNFeed result) {
        // Persist the full feed to this feed's own offline cache (full loads only).
        FileUtil.setLastHNFeed(result, mFeedType);
    }

    public static void startOrReattach(Activity activity, ITaskFinishedHandler<HNFeed> finishedHandler,
            FeedType feedType, int taskCode) {
        HNFeedTask task = getInstance(feedType, taskCode);
        task.setTag(feedType);
        task.setOnFinishedHandler(activity, finishedHandler, HNFeed.class);
        if (!task.isRunning()) {
            task.startInBackground();
        }
    }

    public static void stopCurrent(Context applicationContext, FeedType feedType) {
        getInstance(feedType, 0).cancel();
    }

    public static boolean isRunning(Context applicationContext, FeedType feedType) {
        return getInstance(feedType, 0).isRunning();
    }
}
