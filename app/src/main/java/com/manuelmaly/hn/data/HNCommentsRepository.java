package com.manuelmaly.hn.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.manuelmaly.hn.App;
import com.manuelmaly.hn.Settings;
import com.manuelmaly.hn.model.HNComment;
import com.manuelmaly.hn.model.HNPostComments;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.task.BaseTask;
import com.manuelmaly.hn.task.HNPostCommentsTask;
import com.manuelmaly.hn.task.HNVoteTask;
import com.manuelmaly.hn.util.FileUtil;

import java.io.Serializable;

/**
 * Repository for HN comments data. Bridges the existing BaseTask/LocalBroadcastManager
 * task framework with LiveData for the MVVM layer.
 *
 * Responsibilities:
 * - Triggers comments loading via existing task singletons
 * - Triggers vote operations via HNVoteTask
 * - Listens for task completion broadcasts and posts results to LiveData
 * - Loads cached comments from disk
 */
public class HNCommentsRepository {

    private final Context appContext;
    private final MutableLiveData<Resource<HNPostComments>> commentsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<VoteResult>> voteResultLiveData = new MutableLiveData<>();

    private BroadcastReceiver commentsReceiver;
    private BroadcastReceiver voteReceiver;

    public HNCommentsRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Returns LiveData observing the comments loading state (loading/success/error).
     */
    public LiveData<Resource<HNPostComments>> getComments() {
        return commentsLiveData;
    }

    /**
     * Returns LiveData observing vote operation results.
     */
    public LiveData<Resource<VoteResult>> getVoteResult() {
        return voteResultLiveData;
    }

    /**
     * Triggers loading comments for the given post ID.
     * Results are posted to {@link #getComments()}.
     */
    public void loadComments(String postId) {
        commentsLiveData.postValue(Resource.<HNPostComments>loading(null));
        unregisterCommentsReceiver();

        commentsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode = intent.getIntExtra(BaseTask.BROADCAST_INTENT_EXTRA_ERROR,
                        IAPICommand.ERROR_NONE);
                Serializable rawResult = intent.getSerializableExtra(BaseTask.BROADCAST_INTENT_EXTRA_RESULT);

                if (errorCode == IAPICommand.ERROR_NONE && rawResult instanceof HNPostComments) {
                    commentsLiveData.postValue(Resource.success((HNPostComments) rawResult));
                } else {
                    commentsLiveData.postValue(Resource.<HNPostComments>error(
                            "Failed to load comments", null));
                }
            }
        };

        IntentFilter filter = new IntentFilter(HNPostCommentsTask.BROADCAST_INTENT_ID);
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(commentsReceiver, filter);

        HNPostCommentsTask task = HNPostCommentsTask.getInstance(postId, 0);
        if (!task.isRunning()) {
            task.startInBackground();
        }
    }

    /**
     * Loads cached comments from disk (for immediate display before network returns).
     * Only posts to LiveData if the cached data is for the current user.
     */
    public void loadCachedComments(String postId) {
        new FileUtil.GetLastHNPostCommentsTask() {
            @Override
            protected void onPostExecute(HNPostComments result) {
                boolean registeredUserChanged = result != null
                        && result.getUserAcquiredFor() != null
                        && (!result.getUserAcquiredFor().equals(
                                Settings.getUserName(App.getInstance())));
                if (!registeredUserChanged && result != null) {
                    commentsLiveData.postValue(Resource.success(result));
                }
            }
        }.execute(postId);
    }

    /**
     * Triggers a vote on the given comment.
     * Results are posted to {@link #getVoteResult()}.
     *
     * @param voteURL the vote URL for this comment
     * @param comment the comment being voted on (passed through to the result)
     */
    public void vote(String voteURL, HNComment comment) {
        unregisterVoteReceiver();

        voteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode = intent.getIntExtra(BaseTask.BROADCAST_INTENT_EXTRA_ERROR,
                        IAPICommand.ERROR_NONE);
                Serializable rawResult = intent.getSerializableExtra(BaseTask.BROADCAST_INTENT_EXTRA_RESULT);

                boolean success = (errorCode == IAPICommand.ERROR_NONE
                        && rawResult instanceof Boolean
                        && (Boolean) rawResult);

                VoteResult result = new VoteResult(success, comment);
                if (success) {
                    voteResultLiveData.postValue(Resource.success(result));
                } else {
                    voteResultLiveData.postValue(Resource.error("Vote failed", result));
                }
            }
        };

        IntentFilter filter = new IntentFilter(HNVoteTask.BROADCAST_INTENT_ID);
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(voteReceiver, filter);

        HNVoteTask task = HNVoteTask.getInstance(0);
        if (task.isRunning()) {
            task.cancel();
        }
        task.setTag(comment);
        task.setVoteURL(voteURL);
        task.startInBackground();
    }

    /**
     * Cleans up broadcast receivers. Should be called from ViewModel.onCleared().
     */
    public void dispose() {
        unregisterCommentsReceiver();
        unregisterVoteReceiver();
    }

    private void unregisterCommentsReceiver() {
        if (commentsReceiver != null) {
            LocalBroadcastManager.getInstance(App.getInstance()).unregisterReceiver(commentsReceiver);
            commentsReceiver = null;
        }
    }

    private void unregisterVoteReceiver() {
        if (voteReceiver != null) {
            LocalBroadcastManager.getInstance(App.getInstance()).unregisterReceiver(voteReceiver);
            voteReceiver = null;
        }
    }
}
