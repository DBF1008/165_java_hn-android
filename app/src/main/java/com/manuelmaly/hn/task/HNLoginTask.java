package com.manuelmaly.hn.task;

import android.app.Activity;
import android.util.Log;

import com.manuelmaly.hn.data.network.HNApiClient;
import com.manuelmaly.hn.data.storage.AppSettings;
import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.IAPICommand;

public class HNLoginTask extends BaseTask<Boolean> {

    public static final String BROADCAST_INTENT_ID = "HNLoginTask";

    private static HNLoginTask instance;

    private String mUsername;
    private String mPassword;
    private HNApiClient mApiClient;
    private AppSettings mAppSettings;

    private static HNLoginTask getInstance(int taskCode) {
        synchronized (HNLoginTask.class) {
            if (instance == null)
                instance = new HNLoginTask(taskCode);
        }
        return instance;
    }

    public HNLoginTask(int taskCode) {
        super(BROADCAST_INTENT_ID, taskCode);
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
            ITaskFinishedHandler<Boolean> finishedHandler, int taskCode,
            HNApiClient apiClient, AppSettings appSettings) {
        HNLoginTask task = getInstance(taskCode);
        task.setContext(activity);
        task.mApiClient = apiClient;
        task.mAppSettings = appSettings;
        task.setOnFinishedHandler(activity, finishedHandler, Boolean.class);
        task.setData(username, password);
        if (task.isRunning())
            task.cancel();
        task.startInBackground();
    }

    class HNLoginTaskRunnable extends CancelableRunnable {

        @Override
        public void run() {
            try {
                String userToken = mApiClient.loginAndGetToken(mUsername, mPassword);
                if (userToken != null && !userToken.equals("")) {
                    mResult = true;
                    mAppSettings.setUserData(mUsername, userToken);
                } else {
                    mResult = false;
                }
            } catch (Exception e) {
                Log.e("HNLoginTask", "Login error :(", e);
                mResult = false;
                if (!mCancelled) {
                    mErrorCode = IAPICommand.ERROR_UNKNOWN;
                }
            }
        }

        @Override
        public void onCancelled() {
            // Cancellation handled at command level
        }
    }
}
