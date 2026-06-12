package com.manuelmaly.hn.util;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link MutableLiveData} that delivers each update to a single active observer
 * exactly once. Used for one-shot UI effects (vote success/error toasts, the
 * "please log in" prompt) which must NOT re-fire when an observer re-attaches
 * after a configuration change — the failure mode of using plain LiveData for
 * events. Plain LiveData is still used for the feed/comments <em>data</em>, whose
 * last value SHOULD replay to a re-attached observer.
 *
 * <p>Based on the well-known AndroidX samples SingleLiveEvent pattern.</p>
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private static final String TAG = "SingleLiveEvent";

    private final AtomicBoolean mPending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(LifecycleOwner owner, final Observer<? super T> observer) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.");
        }
        super.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(@Nullable T t) {
                if (mPending.compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@Nullable T t) {
        mPending.set(true);
        super.setValue(t);
    }

    /** Fire an event with no payload. */
    @MainThread
    public void call() {
        setValue(null);
    }
}
