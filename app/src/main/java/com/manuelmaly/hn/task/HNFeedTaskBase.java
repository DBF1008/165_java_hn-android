package com.manuelmaly.hn.task;

import android.util.Log;

import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.parser.IFeedParser;
import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.ApiCommandFactory;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.server.ICredentialsRepository;
import com.manuelmaly.hn.storage.IFeedStore;
import com.manuelmaly.hn.util.IBackgroundExecutor;

public abstract class HNFeedTaskBase extends BaseTask<HNFeed> {

    protected final ApiCommandFactory mCommandFactory;
    protected final IFeedParser mFeedParser;
    protected final IFeedStore mFeedStore;
    protected final ICredentialsRepository mCredentials;

    public HNFeedTaskBase(String notificationBroadcastIntentID, int taskCode, ApiCommandFactory commandFactory,
        IFeedParser feedParser, IFeedStore feedStore, ICredentialsRepository credentials,
        IBackgroundExecutor backgroundExecutor, ITaskResultPublisher publisher) {
        super(notificationBroadcastIntentID, taskCode, publisher, backgroundExecutor);
        mCommandFactory = commandFactory;
        mFeedParser = feedParser;
        mFeedStore = feedStore;
        mCredentials = credentials;
    }

    @Override
    public CancelableRunnable getTask() {
        return new HNFeedTaskRunnable();
    }

    protected abstract String getFeedURL();

    class HNFeedTaskRunnable extends CancelableRunnable {

        IAPICommand<String> mFeedDownload;

        @Override
        public void run() {
            mFeedDownload = mCommandFactory.createFeedDownload(getFeedURL(), mCredentials.getCookieStore());

            mFeedDownload.run();

            if (mCancelled)
                mErrorCode = IAPICommand.ERROR_CANCELLED_BY_USER;
            else
                mErrorCode = mFeedDownload.getErrorCode();

            if (!mCancelled && mErrorCode == IAPICommand.ERROR_NONE) {
                try {
                    mResult = mFeedParser.parse(mFeedDownload.getResponseContent());
                    mBackgroundExecutor.execute(new Runnable() {
                        public void run() {
                            mFeedStore.writeLastFeed(mResult);
                        }
                    });
                } catch (Exception e) {
                    mResult = null;
                    Log.e("HNFeedTask", "HNFeed Parser Error :(", e);
                }
            }

            if (mResult == null)
                mResult = new HNFeed();
        }

        @Override
        public void onCancelled() {
            if (mFeedDownload != null)
                mFeedDownload.cancel();
        }

    }

}
