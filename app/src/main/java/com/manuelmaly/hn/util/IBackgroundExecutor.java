package com.manuelmaly.hn.util;

/**
 * Runs work off the main thread. Injectable replacement for the static
 * {@link Run#inBackground(Runnable)}; tests inject a synchronous implementation so
 * background writes complete before assertions.
 */
public interface IBackgroundExecutor {

    void execute(Runnable r);

}
