package com.manuelmaly.hn.task;

import android.app.Activity;
import android.content.Context;

import com.manuelmaly.hn.App;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.parser.IFeedParser;
import com.manuelmaly.hn.server.ApiCommandFactory;
import com.manuelmaly.hn.server.ICredentialsRepository;
import com.manuelmaly.hn.storage.IFeedStore;
import com.manuelmaly.hn.util.IBackgroundExecutor;

public class HNFeedTaskLoadMore extends HNFeedTaskBase {

    private HNFeed mFeedToAttachResultsTo;

    private static HNFeedTaskLoadMore instance;
    public static final String BROADCAST_INTENT_ID = "HNFeedLoadMore";

    private static HNFeedTaskLoadMore getInstance(int taskCode) {
        synchronized (HNFeedTaskLoadMore.class) {
            if (instance == null)
                instance = App.component().taskFactory().createLoadMore(taskCode);
        }
        return instance;
    }

    HNFeedTaskLoadMore(int taskCode, ApiCommandFactory commandFactory, IFeedParser feedParser, IFeedStore feedStore,
        ICredentialsRepository credentials, IBackgroundExecutor backgroundExecutor, ITaskResultPublisher publisher) {
        super(BROADCAST_INTENT_ID, taskCode, commandFactory, feedParser, feedStore, credentials, backgroundExecutor,
            publisher);
    }

    @Override
    protected String getFeedURL() {
        return mFeedToAttachResultsTo.getNextPageURL();
    }

    public static void start(Activity activity, ITaskFinishedHandler<HNFeed> finishedHandler,
        HNFeed feedToAttachResultsTo, int taskCode) {
        HNFeedTaskLoadMore task = getInstance(taskCode);
        task.setOnFinishedHandler(activity, finishedHandler, HNFeed.class);
        task.setFeedToAttachResultsTo(feedToAttachResultsTo);
        if (task.isRunning())
            task.cancel();
        task.startInBackground();
    }

    public static void stopCurrent(Context applicationContext, int taskCode) {
        getInstance(taskCode).cancel();
    }

    public static boolean isRunning(Context applicationContext, int taskCode) {
        return getInstance(taskCode).isRunning();
    }

    public void setFeedToAttachResultsTo(HNFeed feedToAttachResultsTo) {
        mFeedToAttachResultsTo = feedToAttachResultsTo;
    }

}
