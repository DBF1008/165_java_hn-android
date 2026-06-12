package com.manuelmaly.hn.task;

import android.app.Activity;
import android.util.Log;

import com.manuelmaly.hn.data.network.HNApiClient;
import com.manuelmaly.hn.data.storage.FeedCache;
import com.manuelmaly.hn.model.HNPostComments;
import com.manuelmaly.hn.parser.CommentsParser;
import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.HNCredentials;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.util.Run;

import java.util.HashMap;

public class HNPostCommentsTask extends BaseTask<HNPostComments> {

    public static final String BROADCAST_INTENT_ID = "HNPostComments";
    private static HashMap<String, HNPostCommentsTask> runningInstances = new HashMap<String, HNPostCommentsTask>();

    private String mPostID;
    private HNApiClient mApiClient;
    private CommentsParser mCommentsParser;
    private FeedCache mFeedCache;

    private HNPostCommentsTask(String postID, int taskCode) {
        super(BROADCAST_INTENT_ID, taskCode);
        mPostID = postID;
    }

    private static HNPostCommentsTask getInstance(String postID, int taskCode) {
        synchronized (HNPostCommentsTask.class) {
            if (!runningInstances.containsKey(postID))
                runningInstances.put(postID, new HNPostCommentsTask(postID, taskCode));
        }
        return runningInstances.get(postID);
    }

    public static void startOrReattach(Activity activity, ITaskFinishedHandler<HNPostComments> finishedHandler,
            String postID, int taskCode,
            HNApiClient apiClient, CommentsParser commentsParser, FeedCache feedCache) {
        HNPostCommentsTask task = getInstance(postID, taskCode);
        task.setContext(activity);
        task.mApiClient = apiClient;
        task.mCommentsParser = commentsParser;
        task.mFeedCache = feedCache;
        task.setOnFinishedHandler(activity, finishedHandler, HNPostComments.class);
        if (!task.isRunning())
            task.startInBackground();
    }

    public static void stopCurrent(String postID) {
        getInstance(postID, 0).cancel();
    }

    public static boolean isRunning(String postID) {
        return getInstance(postID, 0).isRunning();
    }

    @Override
    public CancelableRunnable getTask() {
        return new HNPostCommentsTaskRunnable();
    }

    class HNPostCommentsTaskRunnable extends CancelableRunnable {

        @Override
        public void run() {
            try {
                HashMap<String, String> queryParams = new HashMap<String, String>();
                queryParams.put("id", mPostID);

                String html = mApiClient.downloadHtml(
                        "https://news.ycombinator.com/item", queryParams,
                        HNCredentials.getCookieStore(mContext));

                if (mCancelled) {
                    mErrorCode = IAPICommand.ERROR_CANCELLED_BY_USER;
                } else {
                    mResult = mCommentsParser.parse(html);
                    Run.inBackground(new Runnable() {
                        public void run() {
                            mFeedCache.setLastComments(mPostID, mResult);
                        }
                    });
                }
            } catch (Exception e) {
                mResult = null;
                Log.e("HNPostCommentsTask", "Parse error!", e);
                if (!mCancelled) {
                    mErrorCode = IAPICommand.ERROR_UNKNOWN;
                }
            }

            if (mResult == null)
                mResult = new HNPostComments();
        }

        @Override
        public void onCancelled() {
            // Cancellation handled at command level
        }
    }
}
