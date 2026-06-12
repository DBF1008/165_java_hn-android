package com.manuelmaly.hn.storage;

import android.app.Application;
import android.util.Log;

import com.manuelmaly.hn.model.HNFeed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Default {@link IFeedStore} — object-serialization file cache for the last feed,
 * moved out of the former static {@link com.manuelmaly.hn.util.FileUtil}. Methods
 * are synchronous; callers decide whether to run them off the main thread.
 */
@Singleton
public class FileFeedStore implements IFeedStore {

    private static final String LAST_HNFEED_FILENAME = "lastHNFeed";
    private static final String TAG = "FileFeedStore";

    private final Application mApp;

    @Inject
    public FileFeedStore(Application app) {
        mApp = app;
    }

    @Override
    public HNFeed readLastFeed() {
        ObjectInputStream obj = null;
        try {
            obj = new ObjectInputStream(new FileInputStream(getFilePath()));
            Object rawHNFeed = obj.readObject();
            if (rawHNFeed instanceof HNFeed)
                return (HNFeed) rawHNFeed;
        } catch (Exception e) {
            Log.e(TAG, "Could not get last HNFeed from file :(", e);
        } finally {
            if (obj != null) {
                try {
                    obj.close();
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't close last HN feed file :(", e);
                }
            }
        }
        return null;
    }

    @Override
    public void writeLastFeed(HNFeed feed) {
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(new FileOutputStream(getFilePath()));
            os.writeObject(feed);
        } catch (Exception e) {
            Log.e(TAG, "Could not save last HNFeed to file :(", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't close last HN feed file :(", e);
                }
            }
        }
    }

    private String getFilePath() {
        File dataDir = mApp.getFilesDir();
        // Preserves the original File.pathSeparator behaviour from FileUtil so the
        // on-disk path is unchanged (switching to File.separator is a separate
        // follow-up requiring a migration).
        return dataDir.getAbsolutePath() + File.pathSeparator + LAST_HNFEED_FILENAME;
    }

}
