package com.manuelmaly.hn.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuelmaly.hn.data.HNCommentsRepository;
import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.data.VoteResult;
import com.manuelmaly.hn.model.HNComment;
import com.manuelmaly.hn.model.HNPostComments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ViewModel for the comments screen.
 * Holds all comment-related state that survives configuration changes:
 * - Comment tree data (with expand/collapse state)
 * - Voted comments tracking
 * - Pending vote (for login flow)
 * - Loading/error status
 */
public class CommentsViewModel extends AndroidViewModel {

    private final HNCommentsRepository repository;

    /** Current comment data (with tree structure for expand/collapse) */
    private final MutableLiveData<HNPostComments> currentComments =
            new MutableLiveData<>(new HNPostComments());

    /** Comments that have been voted on. Survives configuration changes. */
    private final MutableLiveData<HashSet<HNComment>> votedComments =
            new MutableLiveData<>(new HashSet<HNComment>());

    /** Comment awaiting vote after login. Survives configuration changes. */
    private final MutableLiveData<HNComment> pendingVote = new MutableLiveData<>();

    /** Flattened comment list for display */
    private final MediatorLiveData<List<CommentUiModel>> flatComments = new MediatorLiveData<>();

    public CommentsViewModel(@NonNull Application application) {
        super(application);
        repository = new HNCommentsRepository(application);

        flatComments.addSource(currentComments, comments -> rebuildFlatList());
    }

    /**
     * Package-private constructor for testing with a mock repository.
     */
    CommentsViewModel(@NonNull Application application, HNCommentsRepository repository) {
        super(application);
        this.repository = repository;

        flatComments.addSource(currentComments, comments -> rebuildFlatList());
    }

    /**
     * Returns the flattened comment list for adapter display.
     */
    public LiveData<List<CommentUiModel>> getFlatComments() {
        return flatComments;
    }

    /**
     * Returns the comments loading/error status.
     */
    public LiveData<Resource<HNPostComments>> getCommentsStatus() {
        return repository.getComments();
    }

    /**
     * Returns vote operation results.
     */
    public LiveData<Resource<VoteResult>> getVoteResult() {
        return repository.getVoteResult();
    }

    /**
     * Returns the set of voted comments. Survives configuration changes.
     */
    public LiveData<HashSet<HNComment>> getVotedComments() {
        return votedComments;
    }

    /**
     * Returns the pending vote comment (for login flow). Survives configuration changes.
     */
    public LiveData<HNComment> getPendingVote() {
        return pendingVote;
    }

    /**
     * Returns the current comments data (for expand/collapse operations).
     */
    public LiveData<HNPostComments> getCurrentComments() {
        return currentComments;
    }

    /**
     * Triggers loading comments for the given post.
     */
    public void loadComments(String postId) {
        repository.loadComments(postId);
    }

    /**
     * Loads cached comments from disk for immediate display.
     */
    public void loadCachedComments(String postId) {
        repository.loadCachedComments(postId);
    }

    /**
     * Updates the current comments data (called when new data arrives from repository
     * or when expand/collapse state changes).
     */
    public void updateComments(HNPostComments comments) {
        currentComments.setValue(comments);
    }

    /**
     * Toggles the expand/collapse state of a comment.
     */
    public void toggleCommentExpanded(HNComment comment) {
        HNPostComments comments = currentComments.getValue();
        if (comments != null) {
            comments.toggleCommentExpanded(comment);
            rebuildFlatList();
        }
    }

    /**
     * Toggles the expand/collapse state of an entire comment thread.
     */
    public void toggleThreadExpanded(HNComment comment) {
        HNPostComments comments = currentComments.getValue();
        if (comments != null && comment.getTreeNode() != null) {
            com.manuelmaly.hn.model.HNCommentTreeNode rootNode =
                    comment.getTreeNode().getRootNode();
            comments.toggleCommentExpanded(rootNode.getComment());
            rebuildFlatList();
        }
    }

    /**
     * Records a successful vote on a comment. Survives configuration changes.
     */
    public void recordVote(HNComment comment) {
        HashSet<HNComment> current = votedComments.getValue();
        if (current == null) {
            current = new HashSet<>();
        }
        HashSet<HNComment> updated = new HashSet<>(current);
        updated.add(comment);
        votedComments.setValue(updated);
    }

    /**
     * Triggers a vote on the given comment.
     */
    public void voteOnComment(String voteURL, HNComment comment) {
        repository.vote(voteURL, comment);
    }

    /**
     * Sets a pending vote (used when login is required before voting).
     * Survives configuration changes.
     */
    public void setPendingVote(HNComment comment) {
        pendingVote.setValue(comment);
    }

    /**
     * Clears the pending vote.
     */
    public void clearPendingVote() {
        pendingVote.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.dispose();
    }

    private void rebuildFlatList() {
        HNPostComments comments = currentComments.getValue();
        if (comments == null || comments.getComments() == null) {
            flatComments.setValue(Collections.<CommentUiModel>emptyList());
            return;
        }

        List<CommentUiModel> models = new ArrayList<>();
        for (HNComment comment : comments.getComments()) {
            boolean expanded = comment.getTreeNode() != null && comment.getTreeNode().isExpanded();
            boolean hasChildren = comment.getTreeNode() != null && comment.getTreeNode().hasChildren();
            models.add(new CommentUiModel(
                    comment,
                    comment.getCommentLevel(),
                    expanded,
                    hasChildren));
        }
        flatComments.setValue(models);
    }
}
