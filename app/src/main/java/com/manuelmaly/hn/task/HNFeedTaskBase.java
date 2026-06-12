package com.manuelmaly.hn.task;

import android.util.Log;

import com.manuelmaly.hn.App;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.parser.HNFeedParser;
import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.HNCredentials;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.server.IAPICommand.RequestType;
import com.manuelmaly.hn.server.StringDownloadCommand;

import java.util.HashMap;

public abstract class HNFeedTaskBase extends BaseTask<HNFeed> {

    public HNFeedTaskBase(String notificationBroadcastIntentID, int taskCode) {
        super(notificationBroadcastIntentID, taskCode);
    }

    @Override
    public CancelableRunnable getTask() {
        return new HNFeedTaskRunnable();
    }

    protected abstract String getFeedURL();

    /**
     * Called on a background thread after a feed page has been parsed successfully. Subclasses
     * override this to persist the full feed to the per-feed offline cache. Default is a no-op,
     * which is what load-more wants: it must NOT overwrite the cache with just a partial extra page.
     */
    protected void onResultParsed(HNFeed result) {
    }

    class HNFeedTaskRunnable extends CancelableRunnable {

        StringDownloadCommand mFeedDownload;

        @Override
        public void run() {
            mFeedDownload = new StringDownloadCommand(getFeedURL(), new HashMap<String, String>(), RequestType.GET, false, null,
                App.getInstance(), HNCredentials.getCookieStore(App.getInstance()));

            mFeedDownload.run();

            if (mCancelled)
                mErrorCode = IAPICommand.ERROR_CANCELLED_BY_USER;
            else
                mErrorCode = mFeedDownload.getErrorCode();

            if (!mCancelled && mErrorCode == IAPICommand.ERROR_NONE) {
                HNFeedParser feedParser = new HNFeedParser();
                try {
                    mResult = feedParser.parse(mFeedDownload.getResponseContent());
                    onResultParsed(mResult);
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
            mFeedDownload.cancel();
        }

    }

}
