package com.manuelmaly.hn.testutil;

import com.manuelmaly.hn.concurrent.AppExecutors;

/** {@link AppExecutors} that runs everything inline on the calling thread. */
public class SyncExecutors implements AppExecutors {

    @Override
    public void background(Runnable r) {
        r.run();
    }

    @Override
    public void main(Runnable r) {
        r.run();
    }
}
