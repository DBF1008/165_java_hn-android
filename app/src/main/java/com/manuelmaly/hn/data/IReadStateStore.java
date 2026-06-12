package com.manuelmaly.hn.data;

import java.util.Set;

/**
 * Persistence seam for the "already read" article markers. Production
 * ({@link SharedPrefsReadStateStore}) is backed by SharedPreferences; tests inject
 * a fake. Both methods are synchronous and intended to be called off the main
 * thread by the ViewModel.
 *
 * <p>Extracted from {@code MainActivity}'s {@code @Background loadAlreadyReadCache}
 * / {@code markAsRead}, so the read-state logic is testable and no longer races on
 * a plain {@code HashSet} shared between a background thread and the adapter.</p>
 */
public interface IReadStateStore {

    /**
     * Returns the {@code title.hashCode()} of every article read within the
     * retention window, pruning entries older than that as a side effect.
     */
    Set<Integer> loadReadTitleHashes();

    /** Records that the article with the given title was just read. */
    void markRead(String title);
}
