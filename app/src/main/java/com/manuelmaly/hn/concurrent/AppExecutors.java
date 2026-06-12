package com.manuelmaly.hn.concurrent;

/**
 * Threading seam for the repository layer. Production uses {@link DefaultAppExecutors}
 * (background thread pool + main-thread Handler); unit tests inject a synchronous
 * implementation so repository/ViewModel logic runs inline on the JVM.
 *
 * <p>Keeping this an interface (never {@code new}ed inside a ViewModel/repository)
 * is what allows the MVVM stack to be exercised without the Android framework.</p>
 */
public interface AppExecutors {

    /** Run work off the main thread. */
    void background(Runnable r);

    /** Deliver a result back on the main thread. */
    void main(Runnable r);
}
