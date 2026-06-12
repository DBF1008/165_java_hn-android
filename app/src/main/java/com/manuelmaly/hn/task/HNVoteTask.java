package com.manuelmaly.hn.task;

import android.app.Activity;

import com.manuelmaly.hn.App;
import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.ApiCommandFactory;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.server.ICredentialsRepository;
import com.manuelmaly.hn.util.IBackgroundExecutor;

public class HNVoteTask extends BaseTask<Boolean> {

    public static final String BROADCAST_INTENT_ID = "HNVoteTask";

    private static HNVoteTask instance;

    private String mVoteURL;

    private final ApiCommandFactory mCommandFactory;
    private final ICredentialsRepository mCredentials;

    private static HNVoteTask getInstance(int taskCode) {
        synchronized (HNVoteTask.class) {
            if (instance == null)
                instance = App.component().taskFactory().createVote(taskCode);
        }
        return instance;
    }

    HNVoteTask(int taskCode, ApiCommandFactory commandFactory, ICredentialsRepository credentials,
        IBackgroundExecutor backgroundExecutor, ITaskResultPublisher publisher) {
        super(BROADCAST_INTENT_ID, taskCode, publisher, backgroundExecutor);
        mCommandFactory = commandFactory;
        mCredentials = credentials;
    }

    @Override
    public CancelableRunnable getTask() {
        return new HNVoteTaskRunnable();
    }

    public void setVoteURL(String voteURL) {
        mVoteURL = voteURL;
    }

    public static void start(String voteURL, Activity activity,
        ITaskFinishedHandler<Boolean> finishedHandler, int taskCode, Object tag) {
        HNVoteTask task = getInstance(taskCode);
        task.setTag(tag);
        task.setOnFinishedHandler(activity, finishedHandler, Boolean.class);
        if (task.isRunning())
            task.cancel();
        task.setVoteURL(voteURL);
        task.startInBackground();
    }

    class HNVoteTaskRunnable extends CancelableRunnable {

        IAPICommand<Boolean> mVoteCommand;

        @Override
        public void run() {
            mResult = vote();
        }

        private Boolean vote() {
            mVoteCommand = mCommandFactory.createVote(mVoteURL, mCredentials.getCookieStore());
            mVoteCommand.run();

            // Propagate the command's error code (previously this was never assigned,
            // so failures were silently reported as ERROR_NONE / success).
            if (mCancelled)
                mErrorCode = IAPICommand.ERROR_CANCELLED_BY_USER;
            else
                mErrorCode = mVoteCommand.getErrorCode();

            if (mCancelled || mErrorCode != IAPICommand.ERROR_NONE)
                return null;

            return mVoteCommand.getResponseContent();
        }

        @Override
        public void onCancelled() {
            if (mVoteCommand != null)
                mVoteCommand.cancel();
        }

    }

}
