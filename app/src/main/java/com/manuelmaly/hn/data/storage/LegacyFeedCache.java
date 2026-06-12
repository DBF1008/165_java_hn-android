package com.manuelmaly.hn.data.storage;

import android.content.Context;
import android.util.Log;

import com.manuelmaly.hn.model.HNCommentTreeNode;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPostComments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Legacy implementation that replicates FileUtil's serialization-based caching.
 * Methods are synchronous (callers should invoke from background threads).
 */
@Singleton
public class LegacyFeedCache implements FeedCache {

    private static final String LAST_HNFEED_FILENAME = "lastHNFeed";
    private static final String LAST_HNPOSTCOMMENTS_FILENAME_PREFIX = "lastHNPostComments";
    private static final String TAG = "LegacyFeedCache";

    private final Context context;

    @Inject
    public LegacyFeedCache(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override
    public HNFeed getLastFeed() {
        ObjectInputStream obj = null;
        try {
            obj = new ObjectInputStream(new FileInputStream(getLastHNFeedFilePath()));
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
                    Log.e(TAG, "Couldn't close last NH feed file :(", e);
                }
            }
        }
        return null;
    }

    @Override
    public void setLastFeed(HNFeed feed) {
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(new FileOutputStream(getLastHNFeedFilePath()));
            os.writeObject(feed);
        } catch (Exception e) {
            Log.e(TAG, "Could not save last HNFeed to file :(", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't close last NH feed file :(", e);
                }
            }
        }
    }

    @Override
    public HNPostComments getLastComments(String postId) {
        ObjectInputStream obj = null;
        try {
            obj = new ObjectInputStream(new FileInputStream(getLastHNPostCommentsPath(postId)));
            Object rawHNComments = obj.readObject();
            if (rawHNComments instanceof HNPostComments)
                return (HNPostComments) rawHNComments;
        } catch (Exception e) {
            Log.e(TAG, "Could not get last HNPostComments from file :(", e);
        } finally {
            if (obj != null) {
                try {
                    obj.close();
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't close last NH comments file :(", e);
                }
            }
        }
        return null;
    }

    @Override
    public void setLastComments(String postId, HNPostComments comments) {
        ObjectOutputStream os = null;
        try {
            int nodesCount = countNodes(comments.getTreeNodes());
            if (nodesCount > 150) {
                return;
            }
            os = new ObjectOutputStream(new FileOutputStream(getLastHNPostCommentsPath(postId)));
            os.writeObject(comments);
        } catch (Exception e) {
            Log.e(TAG, "Could not save last HNPostComments to file :(", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't close last NH comments file :(", e);
                }
            }
        }
    }

    private String getLastHNFeedFilePath() {
        File dataDir = context.getFilesDir();
        return dataDir.getAbsolutePath() + File.separator + LAST_HNFEED_FILENAME;
    }

    private String getLastHNPostCommentsPath(String postID) {
        File dataDir = context.getFilesDir();
        return dataDir.getAbsolutePath() + "/" + LAST_HNPOSTCOMMENTS_FILENAME_PREFIX + "_" + postID;
    }

    private int countNodes(List<HNCommentTreeNode> nodes) {
        int sum = 0;
        if (nodes != null) {
            sum += nodes.size();
            for (HNCommentTreeNode n : nodes) {
                sum += countNodes(n.getChildren());
            }
        }
        return sum;
    }
}
