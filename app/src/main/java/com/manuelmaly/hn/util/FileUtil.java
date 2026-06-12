package com.manuelmaly.hn.util;

import android.os.AsyncTask;

import com.manuelmaly.hn.App;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPostComments;

/**
 * Static facade over the offline file cache, kept for source compatibility (the
 * Activities subclass the {@code GetLast*Task} AsyncTasks). The actual read/write
 * logic now lives in the injectable {@code IFeedStore}/{@code ICommentsStore},
 * obtained from the Dagger component. The write helpers keep their off-main-thread
 * contract via {@link Run#inBackground(Runnable)}.
 */
public class FileUtil {

    public abstract static class GetLastHNFeedTask extends AsyncTask<Void, Void, HNFeed> {
        @Override
        protected HNFeed doInBackground(Void... params) {
            return App.component().feedStore().readLastFeed();
        }
    }

    public static void setLastHNFeed(final HNFeed hnFeed) {
        Run.inBackground(new Runnable() {
            public void run() {
                App.component().feedStore().writeLastFeed(hnFeed);
            }
        });
    }

    public abstract static class GetLastHNPostCommentsTask extends AsyncTask<String, Void, HNPostComments> {
        @Override
        protected HNPostComments doInBackground(String... postIDs) {
            if (postIDs != null && postIDs.length > 0)
                return App.component().commentsStore().readLastComments(postIDs[0]);
            return null;
        }
    }

    public static void setLastHNPostComments(final HNPostComments comments, final String postID) {
        Run.inBackground(new Runnable() {
            public void run() {
                App.component().commentsStore().writeLastComments(comments, postID);
            }
        });
    }

}
