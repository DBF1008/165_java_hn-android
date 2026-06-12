package com.manuelmaly.hn.task;

import android.app.Activity;
import android.util.Log;

import com.manuelmaly.hn.App;
import com.manuelmaly.hn.model.HNPostComments;
import com.manuelmaly.hn.parser.ICommentsParser;
import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.ApiCommandFactory;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.server.ICredentialsRepository;
import com.manuelmaly.hn.storage.ICommentsStore;
import com.manuelmaly.hn.util.IBackgroundExecutor;

import java.util.HashMap;

public class HNPostCommentsTask extends BaseTask<HNPostComments> {

    public static final String BROADCAST_INTENT_ID = "HNPostComments";
    private static HashMap<String, HNPostCommentsTask> runningInstances = new HashMap<String, HNPostCommentsTask>();

    private String mPostID; // for which post shall comments be loaded?

    private final ApiCommandFactory mCommandFactory;
    private final ICommentsParser mCommentsParser;
    private final ICommentsStore mCommentsStore;
    private final ICredentialsRepository mCredentials;

    HNPostCommentsTask(String postID, int taskCode, ApiCommandFactory commandFactory, ICommentsParser commentsParser,
        ICommentsStore commentsStore, ICredentialsRepository credentials, IBackgroundExecutor backgroundExecutor,
        ITaskResultPublisher publisher) {
        super(BROADCAST_INTENT_ID, taskCode, publisher, backgroundExecutor);
        mPostID = postID;
        mCommandFactory = commandFactory;
        mCommentsParser = commentsParser;
        mCommentsStore = commentsStore;
        mCredentials = credentials;
    }

    /**
     * I know, Singleton is generally a no-no, but the only other option would be to
     * store the currently running HNPostCommentsTasks in the App object, which I
     * consider far worse. If you find a better solution, please tweet me at
     * @manuelmaly
     *
     * @return
     */
    private static HNPostCommentsTask getInstance(String postID, int taskCode) {
        synchronized (HNPostCommentsTask.class) {
            if (!runningInstances.containsKey(postID))
                runningInstances.put(postID, App.component().taskFactory().createPostComments(postID, taskCode));
        }
        return runningInstances.get(postID);
    }

    public static void startOrReattach(Activity activity, ITaskFinishedHandler<HNPostComments> finishedHandler,
        String postID, int taskCode) {
        HNPostCommentsTask task = getInstance(postID, taskCode);
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

        IAPICommand<String> mFeedDownload;

        @Override
        public void run() {
            mFeedDownload = mCommandFactory.createCommentsDownload(mPostID, mCredentials.getCookieStore());
            mFeedDownload.run();

            if (mCancelled)
                mErrorCode = IAPICommand.ERROR_CANCELLED_BY_USER;
            else
                mErrorCode = mFeedDownload.getErrorCode();

            if (!mCancelled && mErrorCode == IAPICommand.ERROR_NONE) {
                try {
                    mResult = mCommentsParser.parse(mFeedDownload.getResponseContent());
                    mBackgroundExecutor.execute(new Runnable() {
                        public void run() {
                            mCommentsStore.writeLastComments(mResult, mPostID);
                        }
                    });
                } catch (Exception e) {
                    Log.e("HNFeedTask", "Parse error!", e);
                }
            }

            if (mResult == null)
                mResult = new HNPostComments();
        }

        @Override
        public void onCancelled() {
            if (mFeedDownload != null)
                mFeedDownload.cancel();
        }

    }

}
