package com.manuelmaly.hn.data.storage;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Legacy implementation of ReadTracker using SharedPreferences.
 * Logic extracted from MainActivity's loadAlreadyReadCache/markAsRead methods.
 */
@Singleton
public class LegacyReadTracker implements ReadTracker {

    private static final String ALREADY_READ_ARTICLES_KEY = "HN_ALREADY_READ";

    private final Context context;

    @Inject
    public LegacyReadTracker(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override
    public Set<Integer> loadAlreadyRead() {
        Set<Integer> alreadyRead = new HashSet<>();

        SharedPreferences sharedPref = context.getSharedPreferences(
                ALREADY_READ_ARTICLES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Map<String, ?> read = sharedPref.getAll();
        Long now = new Date().getTime();

        for (Map.Entry<String, ?> entry : read.entrySet()) {
            Long readAt = (Long) entry.getValue();
            Long diff = (now - readAt) / (24 * 60 * 60 * 1000);
            if (diff >= 2) {
                editor.remove(entry.getKey());
            } else {
                alreadyRead.add(entry.getKey().hashCode());
            }
        }
        editor.commit();

        return alreadyRead;
    }

    @Override
    public void markAsRead(String postTitle) {
        Long now = new Date().getTime();
        SharedPreferences.Editor editor = context.getSharedPreferences(
                ALREADY_READ_ARTICLES_KEY, Context.MODE_PRIVATE).edit();
        editor.putLong(postTitle, now);
        editor.commit();
    }
}
