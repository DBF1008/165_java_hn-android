package com.manuelmaly.hn.task;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.manuelmaly.hn.reuse.CancelableRunnable;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.task.ITaskFinishedHandler.TaskResultCode;
import com.manuelmaly.hn.util.IBackgroundExecutor;
import com.manuelmaly.hn.util.Run;

import java.io.Serializable;
import java.lang.ref.SoftReference;

/**
 * Generic base for tasks performed asynchronously. Its on-finished-notification is
 * delivered to every Activity instance which has registered (listeners are notified
 * via {@link ITaskResultPublisher}, backed in production by
 * {@link LocalBroadcastManager}). Meaning, there will be no Zombie tasks performing
 * stuff for nothing (e.g. because their callback Activity has been destroyed because
 * of an orientation change).
 *
 * <p>Collaborators ({@link ITaskResultPublisher}, {@link IBackgroundExecutor}) are
 * injected so the task can be unit-tested without the static {@code App}/
 * {@code LocalBroadcastManager}/{@code Run} singletons.
 *
 * @author manuelmaly
 * @param <T> result type
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

    protected final ITaskResultPublisher mPublisher;
    protected final IBackgroundExecutor mBackgroundExecutor;

    public BaseTask(String notificationBroadcastIntentID, int taskCode, ITaskResultPublisher publisher,
        IBackgroundExecutor backgroundExecutor) {
        mNotificationBroadcastIntentID = notificationBroadcastIntentID;
        mTaskCode = taskCode;
        mPublisher = publisher;
        mBackgroundExecutor = backgroundExecutor;
    }

    protected void startInBackground() {
        mBackgroundExecutor.execute(this);
    }

    /**
     * The broadcast will be received by listeners on the main thread implicitly.
     */
    public void notifyFinished(int errorCode, Serializable result) {
        mPublisher.publishFinished(mNotificationBroadcastIntentID, errorCode, result);
    }

    /**
     * @param tag
     */
    public void setTag(Object tag) {
        mTag = tag;
    }

    /**
     * Registers the given {@link BroadcastReceiver} to this task's
     * finished-notification.
     *
     * @param receiver
     */
    public void registerForFinishedNotification(BroadcastReceiver receiver) {
        mPublisher.register(mNotificationBroadcastIntentID, receiver);
    }

    /**
     * Schedules behaviour to be executed when this task has finished, for the given
     * Activity.
     *
     * @param activity
     * @param finishedHandler
     * @param resultClazz
     */
    public void setOnFinishedHandler(Activity activity, ITaskFinishedHandler<T> finishedHandler,
        final Class<T> resultClazz) {
        final SoftReference<Activity> activityRef = new SoftReference<Activity>(activity);
        final SoftReference<ITaskFinishedHandler<T>> finishedHandlerRef = new SoftReference<ITaskFinishedHandler<T>>(
            finishedHandler);
        BroadcastReceiver finishedListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mPublisher.unregister(this);

                if (activityRef == null || activityRef.get() == null || finishedHandlerRef == null
                    || finishedHandlerRef.get() == null)
                    return;

                // Make hard references until the end of processing, so we don't
                // lose those objects:
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
                		// We have no error so far, but cannot cast the result data:
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
        mBackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mTaskRunnable != null)
                    mTaskRunnable.cancel();
            }
        });
    }

    public abstract CancelableRunnable getTask();

}
