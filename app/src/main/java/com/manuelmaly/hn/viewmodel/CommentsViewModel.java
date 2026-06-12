package com.manuelmaly.hn.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.concurrent.AppExecutors;
import com.manuelmaly.hn.data.CommentsRepository;
import com.manuelmaly.hn.data.RepoCallback;
import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.model.HNComment;
import com.manuelmaly.hn.model.HNPostComments;
import com.manuelmaly.hn.util.SingleLiveEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Holds all comments-screen state and data wiring that used to live in
 * {@code CommentsActivity}. Comment data is exposed as {@link LiveData} (replays
 * after a configuration change); toasts are {@link SingleLiveEvent}. The login flow
 * stays in the Activity, so this class needs no {@code Context}/{@code Settings};
 * {@link #setPendingVote(HNComment)} just holds the comment the Activity wants to
 * vote on after a login round-trip.
 */
public class CommentsViewModel extends ViewModel {

    private final CommentsRepository mRepository;
    private final AppExecutors mExecutors;

    private final MutableLiveData<Resource<HNPostComments>> mComments =
            new MutableLiveData<Resource<HNPostComments>>();
    private final MutableLiveData<Boolean> mRefreshing = new MutableLiveData<Boolean>();
    private final SingleLiveEvent<Boolean> mVoteEvent = new SingleLiveEvent<Boolean>();
    private final SingleLiveEvent<Integer> mErrorMessage = new SingleLiveEvent<Integer>();

    /** Comments voted on this session (identity-based: HNComment has no equals). */
    private final Set<HNComment> mVotedComments = new HashSet<HNComment>();
    private HNComment mPendingVote;

    public CommentsViewModel(CommentsRepository repository, AppExecutors executors) {
        mRepository = repository;
        mExecutors = executors;
        mComments.setValue(Resource.success(new HNPostComments()));
        mRefreshing.setValue(false);
    }

    // --- Observable state -------------------------------------------------

    public LiveData<Resource<HNPostComments>> getComments() {
        return mComments;
    }

    public LiveData<Boolean> getRefreshing() {
        return mRefreshing;
    }

    public SingleLiveEvent<Boolean> getVoteEvent() {
        return mVoteEvent;
    }

    public SingleLiveEvent<Integer> getErrorMessage() {
        return mErrorMessage;
    }

    // --- Actions ----------------------------------------------------------

    public void loadComments(String postId) {
        mRepository.loadComments(postId, currentComments(), new RepoCallback<HNPostComments>() {
            @Override
            public void onResult(Resource<HNPostComments> resource) {
                mComments.setValue(resource);
                mRefreshing.setValue(resource.isLoading());
                if (resource.isError()) {
                    mErrorMessage.setValue(R.string.error_unable_to_retrieve_comments);
                }
            }
        });
    }

    /** Loads cached comments as an intermediate result, gated to the current user. */
    public void loadCached(String postId, final String currentUserName) {
        mRepository.loadCachedComments(postId, new RepoCallback<HNPostComments>() {
            @Override
            public void onResult(Resource<HNPostComments> resource) {
                HNPostComments cached = resource.getData();
                if (cached == null) {
                    return;
                }
                boolean userChanged = cached.getUserAcquiredFor() != null
                        && !cached.getUserAcquiredFor().equals(currentUserName);
                if (userChanged) {
                    return;
                }
                HNPostComments current = currentComments();
                if (current == null || current.getComments() == null || current.getComments().isEmpty()) {
                    mComments.setValue(Resource.success(cached));
                }
            }
        });
    }

    public void vote(String voteUrl, final HNComment comment) {
        mRepository.vote(voteUrl, new RepoCallback<Boolean>() {
            @Override
            public void onResult(Resource<Boolean> resource) {
                boolean accepted = resource.isSuccess() && Boolean.TRUE.equals(resource.getData());
                if (accepted && comment != null) {
                    mVotedComments.add(comment);
                }
                mVoteEvent.setValue(accepted);
            }
        });
    }

    /** Collapses/expands the subtree rooted at the given comment. */
    public void toggleExpanded(HNComment comment) {
        HNPostComments current = currentComments();
        if (current != null) {
            current.toggleCommentExpanded(comment);
            mComments.setValue(Resource.success(current));
        }
    }

    /** Collapses the whole thread the given comment belongs to. */
    public void collapseThread(HNComment comment) {
        HNPostComments current = currentComments();
        if (current != null && comment != null && comment.getTreeNode() != null) {
            HNComment root = comment.getTreeNode().getRootNode().getComment();
            current.toggleCommentExpanded(root);
            mComments.setValue(Resource.success(current));
        }
    }

    public void setPendingVote(HNComment comment) {
        mPendingVote = comment;
    }

    public HNComment getPendingVote() {
        return mPendingVote;
    }

    // --- Helpers for the view ---------------------------------------------

    public HNPostComments currentComments() {
        Resource<HNPostComments> resource = mComments.getValue();
        return resource != null ? resource.getData() : null;
    }

    public boolean isVoted(HNComment comment) {
        return mVotedComments.contains(comment);
    }
}
