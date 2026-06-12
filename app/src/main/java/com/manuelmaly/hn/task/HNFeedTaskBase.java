package com.manuelmaly.hn.task;

import android.util.Log;

import com.manuelmaly.hn.data.network.HNApiClient;
import com.manuelmaly.hn.data.storage.FeedCache;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.parser.FeedParser;
import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.HNCredentials;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.util.Run;

import java.util.HashMap;

public abstract class HNFeedTaskBase extends BaseTask<HNFeed> {

    protected HNApiClient mApiClient;
    protected FeedParser mFeedParser;
    protected FeedCache mFeedCache;

    public HNFeedTaskBase(String notificationBroadcastIntentID, int taskCode) {
        super(notificationBroadcastIntentID, taskCode);
    }

    public void setDependencies(HNApiClient apiClient, FeedParser feedParser, FeedCache feedCache) {
        mApiClient = apiClient;
        mFeedParser = feedParser;
        mFeedCache = feedCache;
    }

    @Override
    public CancelableRunnable getTask() {
        return new HNFeedTaskRunnable();
    }

    protected abstract String getFeedURL();

    class HNFeedTaskRunnable extends CancelableRunnable {

        @Override
        public void run() {
            try {
                String html = mApiClient.downloadHtml(
                        getFeedURL(), new HashMap<String, String>(),
                        HNCredentials.getCookieStore(mContext));

                if (mCancelled) {
                    mErrorCode = IAPICommand.ERROR_CANCELLED_BY_USER;
                } else {
                    mResult = mFeedParser.parse(html);
                    final HNFeed feedResult = mResult;
                    Run.inBackground(new Runnable() {
                        public void run() {
                            mFeedCache.setLastFeed(feedResult);
                        }
                    });
                }
            } catch (Exception e) {
                mResult = null;
                Log.e("HNFeedTask", "Feed download/parse error :(", e);
                if (!mCancelled) {
                    mErrorCode = IAPICommand.ERROR_UNKNOWN;
                }
            }

            if (mResult == null)
                mResult = new HNFeed();
        }

        @Override
        public void onCancelled() {
            // Cancellation is handled at the command level if needed
        }
    }
}
