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

public class HNFeedTaskMainFeed extends HNFeedTaskBase {

    private static HNFeedTaskMainFeed instance;
    public static final String BROADCAST_INTENT_ID = "HNFeedMain";

    private static HNFeedTaskMainFeed getInstance(int taskCode) {
        synchronized (HNFeedTaskBase.class) {
            if (instance == null)
                instance = App.component().taskFactory().createMainFeed(taskCode);
        }
        return instance;
    }

    HNFeedTaskMainFeed(int taskCode, ApiCommandFactory commandFactory, IFeedParser feedParser, IFeedStore feedStore,
        ICredentialsRepository credentials, IBackgroundExecutor backgroundExecutor, ITaskResultPublisher publisher) {
        super(BROADCAST_INTENT_ID, taskCode, commandFactory, feedParser, feedStore, credentials, backgroundExecutor,
            publisher);
    }

    @Override
    protected String getFeedURL() {
        return "https://news.ycombinator.com/";
    }

    public static void startOrReattach(Activity activity, ITaskFinishedHandler<HNFeed> finishedHandler, int taskCode) {
        HNFeedTaskMainFeed task = getInstance(taskCode);
        task.setOnFinishedHandler(activity, finishedHandler, HNFeed.class);
        if (!task.isRunning())
            task.startInBackground();
    }

    public static void stopCurrent(Context applicationContext) {
        getInstance(0).cancel();
    }

    public static boolean isRunning(Context applicationContext) {
        return getInstance(0).isRunning();
    }

}
