package com.manuelmaly.hn.testutil;

import com.manuelmaly.hn.concurrent.AppExecutors;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * {@link AppExecutors} that queues background work until {@link #drain()} is called,
 * so tests can assert state <em>between</em> dispatch and completion (e.g. the
 * load-more in-flight flag). Main-thread work runs inline, so once background work
 * runs its result is delivered synchronously.
 */
public class ManualExecutor implements AppExecutors {

    private final Queue<Runnable> mBackground = new ArrayDeque<Runnable>();

    @Override
    public void background(Runnable r) {
        mBackground.add(r);
    }

    @Override
    public void main(Runnable r) {
        r.run();
    }

    public boolean hasPending() {
        return !mBackground.isEmpty();
    }

    /** Runs all queued background tasks (which may enqueue more). */
    public void drain() {
        while (!mBackground.isEmpty()) {
            mBackground.poll().run();
        }
    }
}
