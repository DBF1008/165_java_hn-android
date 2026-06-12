package com.manuelmaly.hn.task;

import android.app.Activity;

import com.manuelmaly.hn.App;
import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.ApiCommandFactory;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.storage.ISettingsRepository;
import com.manuelmaly.hn.util.IBackgroundExecutor;

public class HNLoginTask extends BaseTask<Boolean> {

    public static final String BROADCAST_INTENT_ID = "HNLoginTask";

    private static HNLoginTask instance;

    private String mUsername;
    private String mPassword;

    private final ApiCommandFactory mCommandFactory;
    private final ISettingsRepository mSettings;

    private static HNLoginTask getInstance(int taskCode) {
        synchronized (HNLoginTask.class) {
            if (instance == null)
                instance = App.component().taskFactory().createLogin(taskCode);
        }
        return instance;
    }

    HNLoginTask(int taskCode, ApiCommandFactory commandFactory, ISettingsRepository settings,
        IBackgroundExecutor backgroundExecutor, ITaskResultPublisher publisher) {
        super(BROADCAST_INTENT_ID, taskCode, publisher, backgroundExecutor);
        mCommandFactory = commandFactory;
        mSettings = settings;
    }

    @Override
    public CancelableRunnable getTask() {
        return new HNLoginTaskRunnable();
    }

    public void setData(String username, String password) {
        mUsername = username;
        mPassword = password;
    }

    public static void start(String username, String password, Activity activity,
        ITaskFinishedHandler<Boolean> finishedHandler, int taskCode) {
        HNLoginTask task = getInstance(taskCode);
        task.setOnFinishedHandler(activity, finishedHandler, Boolean.class);
        task.setData(username, password);
        if (task.isRunning())
            task.cancel();
        task.startInBackground();
    }

    class HNLoginTaskRunnable extends CancelableRunnable {

        IAPICommand<String> getUserTokenCommand;

        @Override
        public void run() {
            String userToken = getUserToken();
            if (userToken != null && !userToken.equals("")) {
                mResult = true;
                mSettings.setUserData(mUsername, userToken);
            }
            else
                mResult = false;
        }

        private String getUserToken() {
            getUserTokenCommand = mCommandFactory.createLoginToken(mUsername, mPassword);
            getUserTokenCommand.run();

            if (mCancelled)
                mErrorCode = IAPICommand.ERROR_CANCELLED_BY_USER;
            else
                mErrorCode = getUserTokenCommand.getErrorCode();

            if (!mCancelled && mErrorCode == IAPICommand.ERROR_NONE) {
                return getUserTokenCommand.getResponseContent();
            }
            return null;
        }

        @Override
        public void onCancelled() {
            if (getUserTokenCommand != null)
                getUserTokenCommand.cancel();
        }

    }

}
