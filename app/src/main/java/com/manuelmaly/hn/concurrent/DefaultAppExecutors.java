package com.manuelmaly.hn.concurrent;

import android.os.Handler;
import android.os.Looper;

import com.manuelmaly.hn.util.Run;

/**
 * Production {@link AppExecutors}: reuses the existing {@link Run} cached thread pool
 * for background work and a main-looper {@link Handler} for delivery. This mirrors
 * the threading the old task framework used ({@code Run.inBackground} +
 * {@code Activity.runOnUiThread}) minus the Activity dependency.
 */
public class DefaultAppExecutors implements AppExecutors {

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void background(Runnable r) {
        Run.inBackground(r);
    }

    @Override
    public void main(Runnable r) {
        mMainHandler.post(r);
    }
}
