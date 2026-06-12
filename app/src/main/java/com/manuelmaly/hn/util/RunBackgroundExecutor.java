package com.manuelmaly.hn.util;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Default {@link IBackgroundExecutor}, delegating to {@link Run#inBackground}.
 */
@Singleton
public class RunBackgroundExecutor implements IBackgroundExecutor {

    @Inject
    public RunBackgroundExecutor() {
    }

    @Override
    public void execute(Runnable r) {
        Run.inBackground(r);
    }

}
