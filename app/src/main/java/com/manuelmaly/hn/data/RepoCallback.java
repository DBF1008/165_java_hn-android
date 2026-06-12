package com.manuelmaly.hn.data;

/**
 * Main-thread callback used by the repositories to deliver a {@link Resource} back
 * to a ViewModel. Replaces the old {@code ITaskFinishedHandler} broadcast result.
 */
public interface RepoCallback<T> {
    void onResult(Resource<T> resource);
}
