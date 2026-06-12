package com.manuelmaly.hn.task;

import android.app.Activity;

import com.manuelmaly.hn.data.network.HNApiClient;
import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.HNCredentials;
import com.manuelmaly.hn.server.IAPICommand;

public class HNVoteTask extends BaseTask<Boolean> {

    public static final String BROADCAST_INTENT_ID = "HNVoteTask";

    private static HNVoteTask instance;

    private String mVoteURL;
    private HNApiClient mApiClient;

    private static HNVoteTask getInstance(int taskCode) {
        synchronized (HNVoteTask.class) {
            if (instance == null)
                instance = new HNVoteTask(taskCode);
        }
        return instance;
    }

    public HNVoteTask(int taskCode) {
        super(BROADCAST_INTENT_ID, taskCode);
    }

    @Override
    public CancelableRunnable getTask() {
        return new HNVoteTaskRunnable();
    }

    public void setVoteURL(String voteURL) {
        mVoteURL = voteURL;
    }

    public static void start(String voteURL, Activity activity,
            ITaskFinishedHandler<Boolean> finishedHandler, int taskCode, Object tag,
            HNApiClient apiClient) {
        HNVoteTask task = getInstance(taskCode);
        task.setContext(activity);
        task.mApiClient = apiClient;
        task.setTag(tag);
        task.setOnFinishedHandler(activity, finishedHandler, Boolean.class);
        if (task.isRunning())
            task.cancel();
        task.setVoteURL(voteURL);
        task.startInBackground();
    }

    class HNVoteTaskRunnable extends CancelableRunnable {

        @Override
        public void run() {
            mResult = vote();
        }

        private Boolean vote() {
            try {
                Boolean result = mApiClient.vote(mVoteURL,
                        HNCredentials.getCookieStore(mContext));

                if (mCancelled || result == null)
                    return null;

                return result;
            } catch (Exception e) {
                mErrorCode = IAPICommand.ERROR_UNKNOWN;
                return null;
            }
        }

        @Override
        public void onCancelled() {
            // Cancellation handled at command level
        }
    }
}
