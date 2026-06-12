package com.manuelmaly.hn.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * SharedPreferences-backed {@link IReadStateStore}. Faithfully reproduces the old
 * {@code MainActivity} behavior: read markers are stored as {@code title -> epochMs}
 * under the {@code HN_ALREADY_READ} prefs file, pruned after two days, and surfaced
 * to the UI as {@code title.hashCode()} values.
 */
public class SharedPrefsReadStateStore implements IReadStateStore {

    private static final String PREFS_NAME = "HN_ALREADY_READ";
    private static final long RETENTION_DAYS = 2;
    private static final long MS_PER_DAY = 24L * 60 * 60 * 1000;

    private final Context mAppContext;

    public SharedPrefsReadStateStore(Context context) {
        mAppContext = context.getApplicationContext();
    }

    @Override
    public Set<Integer> loadReadTitleHashes() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> read = prefs.getAll();
        long now = System.currentTimeMillis();

        Set<Integer> hashes = new HashSet<Integer>();
        for (Map.Entry<String, ?> entry : read.entrySet()) {
            Long readAt = (Long) entry.getValue();
            long diffDays = (now - readAt) / MS_PER_DAY;
            if (diffDays >= RETENTION_DAYS) {
                editor.remove(entry.getKey());
            } else {
                hashes.add(entry.getKey().hashCode());
            }
        }
        editor.apply();
        return hashes;
    }

    @Override
    public void markRead(String title) {
        mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(title, System.currentTimeMillis())
                .apply();
    }
}
