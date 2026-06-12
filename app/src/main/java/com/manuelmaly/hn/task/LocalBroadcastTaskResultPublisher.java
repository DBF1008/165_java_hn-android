package com.manuelmaly.hn.task;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.Serializable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Default {@link ITaskResultPublisher}, backed by {@code LocalBroadcastManager}.
 * Reproduces the exact intent format the former {@link BaseTask} used so existing
 * Activity receivers keep working unchanged.
 */
@Singleton
public class LocalBroadcastTaskResultPublisher implements ITaskResultPublisher {

    private final Application mApp;

    @Inject
    public LocalBroadcastTaskResultPublisher(Application app) {
        mApp = app;
    }

    @Override
    public void publishFinished(String intentId, int errorCode, Serializable result) {
        Intent broadcastIntent = new Intent(intentId);
        broadcastIntent.putExtra(BaseTask.BROADCAST_INTENT_EXTRA_ERROR, errorCode);
        broadcastIntent.putExtra(BaseTask.BROADCAST_INTENT_EXTRA_RESULT, result);
        LocalBroadcastManager.getInstance(mApp).sendBroadcast(broadcastIntent);
    }

    @Override
    public void register(String intentId, BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter(intentId);
        LocalBroadcastManager.getInstance(mApp).registerReceiver(receiver, filter);
    }

    @Override
    public void unregister(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(mApp).unregisterReceiver(receiver);
    }

}
