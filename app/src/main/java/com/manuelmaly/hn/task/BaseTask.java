package com.manuelmaly.hn.task;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.task.ITaskFinishedHandler.TaskResultCode;
import com.manuelmaly.hn.util.Run;

import java.io.Serializable;
import java.lang.ref.SoftReference;

/**
 * Generic base for tasks performed asynchronously. Unlike {@link AsyncTask},
 * its on-finished-notification will be passed to every Activity instance which
 * has registered (listeners are notified via an intent sent to
 * {@link LocalBroadcastManager}). Meaning, there will be no Zombie tasks
 * performing stuff for nothing (e.g. because their callback Activity has been
 * destroyed because of orientation change).
 */
public abstract class BaseTask<T extends Serializable> implements Runnable {

    public static final String BROADCAST_INTENT_EXTRA_ERROR = "error";
    public static final String BROADCAST_INTENT_EXTRA_RESULT = "result";

    protected String mNotificationBroadcastIntentID;
    protected T mResult;
    protected int mErrorCode;
    protected boolean mIsRunning;
    protected CancelableRunnable mTaskRunnable;
    protected int mTaskCode;
    protected Object mTag;
    protected Context mContext;

    public BaseTask(String notificationBroadcastIntentID, int taskCode) {
        mNotificationBroadcastIntentID = notificationBroadcastIntentID;
        mTaskCode = taskCode;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    protected Context getContext() {
        return mContext;
    }

    protected void startInBackground() {
        Run.inBackground(this);
    }

    public void notifyFinished(int errorCode, Serializable result) {
        Intent broadcastIntent = new Intent(mNotificationBroadcastIntentID);
        broadcastIntent.putExtra(BROADCAST_INTENT_EXTRA_ERROR, errorCode);
        broadcastIntent.putExtra(BROADCAST_INTENT_EXTRA_RESULT, result);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(broadcastIntent);
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public void registerForFinishedNotification(BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter(mNotificationBroadcastIntentID);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, filter);
    }

    public void setOnFinishedHandler(Activity activity, ITaskFinishedHandler<T> finishedHandler,
        final Class<T> resultClazz) {
        final SoftReference<Activity> activityRef = new SoftReference<Activity>(activity);
        final SoftReference<ITaskFinishedHandler<T>> finishedHandlerRef = new SoftReference<ITaskFinishedHandler<T>>(
            finishedHandler);
        BroadcastReceiver finishedListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this);

                if (activityRef == null || activityRef.get() == null || finishedHandlerRef == null
                    || finishedHandlerRef.get() == null)
                    return;

                Activity activity = activityRef.get();
                final ITaskFinishedHandler<T> finishedHandler = finishedHandlerRef.get();

                int lowLevelErrorCode = intent.getIntExtra(BaseTask.BROADCAST_INTENT_EXTRA_ERROR,
                    IAPICommand.ERROR_NONE);
                final int errorCode;
                final T result;
                Serializable rawResult = intent.getSerializableExtra(BaseTask.BROADCAST_INTENT_EXTRA_RESULT);
                if (resultClazz.isInstance(rawResult)) {
                    result = resultClazz.cast(rawResult);
                    errorCode = lowLevelErrorCode;
                } else {
                    result = null;
                    if (lowLevelErrorCode == IAPICommand.ERROR_NONE) {
                        errorCode = IAPICommand.ERROR_UNKNOWN;
                    } else {
                        errorCode = lowLevelErrorCode;
                    }
                }

                Runnable r = new Runnable() {
                    public void run() {
                        finishedHandler
                            .onTaskFinished(mTaskCode, TaskResultCode.fromErrorCode(errorCode), result, mTag);
                    }
                };
                Run.onUiThread(r, activity);
            }
        };
        this.registerForFinishedNotification(finishedListener);
    }

    @Override
    public void run() {
        mIsRunning = true;
        mTaskRunnable = getTask();
        mTaskRunnable.run();
        mIsRunning = false;
        notifyFinished(mErrorCode, mResult);
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public T getResult() {
        return mResult;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public void cancel() {
        Run.inBackground(new Runnable() {
            @Override
            public void run() {
                if (mTaskRunnable != null)
                    mTaskRunnable.cancel();
            }
        });
    }

    public abstract CancelableRunnable getTask();

}
