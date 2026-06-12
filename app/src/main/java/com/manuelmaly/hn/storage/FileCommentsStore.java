package com.manuelmaly.hn.storage;

import android.app.Application;
import android.util.Log;

import com.manuelmaly.hn.model.HNCommentTreeNode;
import com.manuelmaly.hn.model.HNPostComments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Default {@link ICommentsStore} — object-serialization file cache for the last
 * comments per post, moved out of the former static
 * {@link com.manuelmaly.hn.util.FileUtil}. Skips persisting very large trees
 * (&gt; 150 nodes), same as the legacy behaviour. Methods are synchronous.
 */
@Singleton
public class FileCommentsStore implements ICommentsStore {

    private static final String LAST_HNPOSTCOMMENTS_FILENAME_PREFIX = "lastHNPostComments";
    private static final String TAG = "FileCommentsStore";

    private final Application mApp;

    @Inject
    public FileCommentsStore(Application app) {
        mApp = app;
    }

    @Override
    public HNPostComments readLastComments(String postId) {
        ObjectInputStream obj = null;
        try {
            obj = new ObjectInputStream(new FileInputStream(getPath(postId)));
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
                    Log.e(TAG, "Couldn't close last HN comments file :(", e);
                }
            }
        }
        return null;
    }

    @Override
    public void writeLastComments(HNPostComments comments, String postId) {
        ObjectOutputStream os = null;
        try {
            int nodesCount = countNodes(comments.getTreeNodes());
            if (nodesCount > 150) {
                return;
            }
            os = new ObjectOutputStream(new FileOutputStream(getPath(postId)));
            os.writeObject(comments);
        } catch (Exception e) {
            Log.e(TAG, "Could not save last HNPostComments to file :(", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't close last HN comments file :(", e);
                }
            }
        }
    }

    private String getPath(String postId) {
        File dataDir = mApp.getFilesDir();
        return dataDir.getAbsolutePath() + "/" + LAST_HNPOSTCOMMENTS_FILENAME_PREFIX + "_" + postId;
    }

    private static int countNodes(List<HNCommentTreeNode> nodes) {
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
