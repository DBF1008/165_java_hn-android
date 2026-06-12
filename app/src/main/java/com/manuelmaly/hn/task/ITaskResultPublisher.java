package com.manuelmaly.hn.task;

import android.content.BroadcastReceiver;

import java.io.Serializable;

/**
 * Encapsulates how a finished task notifies listeners. Production uses
 * {@code LocalBroadcastManager}; tests use a recording implementation. This keeps
 * {@link BaseTask} free of the static {@code App.getInstance()} /
 * {@code LocalBroadcastManager} coupling.
 */
public interface ITaskResultPublisher {

    /**
     * Broadcasts a task's result. Reproduces the legacy intent format: action =
     * {@code intentId}, extras {@link BaseTask#BROADCAST_INTENT_EXTRA_ERROR} and
     * {@link BaseTask#BROADCAST_INTENT_EXTRA_RESULT}.
     */
    void publishFinished(String intentId, int errorCode, Serializable result);

    void register(String intentId, BroadcastReceiver receiver);

    void unregister(BroadcastReceiver receiver);

}
