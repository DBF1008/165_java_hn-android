package com.manuelmaly.hn.data.storage;

import java.util.Set;

/**
 * Abstraction over the "already read" article tracking stored in SharedPreferences.
 * Extracted from MainActivity's loadAlreadyReadCache/markAsRead methods.
 */
public interface ReadTracker {

    /**
     * Loads the set of already-read post title hashes,
     * purging entries older than 2 days.
     */
    Set<Integer> loadAlreadyRead();

    /**
     * Marks a post as read by storing its title with a timestamp.
     */
    void markAsRead(String postTitle);
}
