package com.manuelmaly.hn.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuelmaly.hn.data.HNCommentsRepository;
import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.data.VoteResult;
import com.manuelmaly.hn.model.HNComment;
import com.manuelmaly.hn.model.HNCommentTreeNode;
import com.manuelmaly.hn.model.HNPostComments;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Unit tests for {@link CommentsViewModel}.
 * Verifies comment loading, vote state persistence across configuration changes,
 * and expand/collapse behavior.
 */
public class CommentsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private Application application;
    private HNCommentsRepository mockRepository;
    private CommentsViewModel viewModel;

    private MutableLiveData<Resource<HNPostComments>> commentsLiveData;
    private MutableLiveData<Resource<VoteResult>> voteResultLiveData;

    @Before
    public void setUp() {
        application = mock(Application.class);
        mockRepository = mock(HNCommentsRepository.class);

        commentsLiveData = new MutableLiveData<>();
        voteResultLiveData = new MutableLiveData<>();

        when(mockRepository.getComments()).thenReturn(commentsLiveData);
        when(mockRepository.getVoteResult()).thenReturn(voteResultLiveData);

        viewModel = new CommentsViewModel(application, mockRepository);
    }

    @Test
    public void loadComments_delegatesToRepository() {
        viewModel.loadComments("12345");
        verify(mockRepository).loadComments("12345");
    }

    @Test
    public void loadCachedComments_delegatesToRepository() {
        viewModel.loadCachedComments("12345");
        verify(mockRepository).loadCachedComments("12345");
    }

    @Test
    public void commentsLoading_emitsLoadingThenSuccess() {
        List<Resource<HNPostComments>> observed = new ArrayList<>();
        viewModel.getCommentsStatus().observeForever(resource -> observed.add(resource));

        // Simulate loading
        commentsLiveData.postValue(Resource.loading(null));
        assertEquals(1, observed.size());
        assertEquals(Resource.Status.LOADING, observed.get(0).status);

        // Simulate success
        HNPostComments comments = createTestComments("Comment 1", "Comment 2");
        commentsLiveData.postValue(Resource.success(comments));
        assertEquals(2, observed.size());
        assertEquals(Resource.Status.SUCCESS, observed.get(1).status);
    }

    @Test
    public void updateComments_rebuildsFlatList() {
        List<List<CommentUiModel>> observed = new ArrayList<>();
        viewModel.getFlatComments().observeForever(comments -> observed.add(comments));

        HNPostComments comments = createTestComments("First", "Second", "Third");
        viewModel.updateComments(comments);

        List<CommentUiModel> latest = observed.get(observed.size() - 1);
        assertEquals(3, latest.size());
        assertEquals("First", latest.get(0).comment.getText());
    }

    @Test
    public void toggleCommentExpanded_updatesFlatList() {
        List<List<CommentUiModel>> observed = new ArrayList<>();
        viewModel.getFlatComments().observeForever(comments -> observed.add(comments));

        HNPostComments comments = createTestComments("Parent");
        // Add a child to make it expandable
        HNComment parent = comments.getComments().get(0);
        HNComment child = createTestComment("Child", 1);
        HNCommentTreeNode parentNode = parent.getTreeNode();
        HNCommentTreeNode childNode = new HNCommentTreeNode(child);
        child.setTreeNode(childNode);
        parentNode.addChild(childNode);

        viewModel.updateComments(comments);

        // Verify initial state: both parent and child visible
        List<CommentUiModel> beforeToggle = observed.get(observed.size() - 1);
        assertEquals(2, beforeToggle.size());
        assertTrue(beforeToggle.get(0).isExpanded);

        // Toggle collapse
        viewModel.toggleCommentExpanded(parent);

        // After collapse: only parent visible, not expanded
        List<CommentUiModel> afterToggle = observed.get(observed.size() - 1);
        assertEquals(1, afterToggle.size());
        assertFalse(afterToggle.get(0).isExpanded);
    }

    @Test
    public void vote_survivesObserverRecreation() {
        // Simulate a successful vote
        HNComment comment = createTestComment("Voted Comment", 0);
        viewModel.recordVote(comment);

        // Verify the voted comments set contains the comment
        HashSet<HNComment> voted = viewModel.getVotedComments().getValue();
        assertNotNull(voted);
        assertTrue(voted.contains(comment));

        // Simulate re-observation (configuration change)
        List<HashSet<HNComment>> observed = new ArrayList<>();
        viewModel.getVotedComments().observeForever(v -> observed.add(v));

        HashSet<HNComment> latest = observed.get(observed.size() - 1);
        assertNotNull(latest);
        assertTrue("Voted comment should survive re-observation", latest.contains(comment));
    }

    @Test
    public void pendingVote_survivesObserverRecreation() {
        // Set a pending vote
        HNComment pendingComment = createTestComment("Pending Vote Comment", 0);
        viewModel.setPendingVote(pendingComment);

        // Verify
        HNComment pending = viewModel.getPendingVote().getValue();
        assertNotNull(pending);
        assertEquals("Pending Vote Comment", pending.getText());

        // Simulate re-observation (configuration change)
        List<HNComment> observed = new ArrayList<>();
        viewModel.getPendingVote().observeForever(p -> observed.add(p));

        HNComment latest = observed.get(observed.size() - 1);
        assertNotNull("Pending vote should survive re-observation", latest);
        assertEquals("Pending Vote Comment", latest.getText());
    }

    @Test
    public void clearPendingVote_setsNull() {
        HNComment comment = createTestComment("Test", 0);
        viewModel.setPendingVote(comment);
        assertNotNull(viewModel.getPendingVote().getValue());

        viewModel.clearPendingVote();
        assertNull(viewModel.getPendingVote().getValue());
    }

    @Test
    public void voteOnComment_delegatesToRepository() {
        HNComment comment = createTestComment("Test", 0);
        viewModel.voteOnComment("https://vote.url", comment);
        verify(mockRepository).vote("https://vote.url", comment);
    }

    @Test
    public void multipleVotes_allTracked() {
        HNComment comment1 = createTestComment("Comment 1", 0);
        HNComment comment2 = createTestComment("Comment 2", 0);
        HNComment comment3 = createTestComment("Comment 3", 0);

        viewModel.recordVote(comment1);
        viewModel.recordVote(comment2);
        viewModel.recordVote(comment3);

        HashSet<HNComment> voted = viewModel.getVotedComments().getValue();
        assertNotNull(voted);
        assertEquals(3, voted.size());
        assertTrue(voted.contains(comment1));
        assertTrue(voted.contains(comment2));
        assertTrue(voted.contains(comment3));
    }

    @Test
    public void onCleared_disposesRepository() {
        viewModel.onCleared();
        verify(mockRepository).dispose();
    }

    // --- Helpers ---

    private HNPostComments createTestComments(String... texts) {
        List<HNComment> comments = new ArrayList<>();
        for (String text : texts) {
            comments.add(createTestComment(text, 0));
        }
        return new HNPostComments(comments, null, "testUser");
    }

    private HNComment createTestComment(String text, int level) {
        HNComment comment = new HNComment("2 hours ago", "testAuthor",
                "https://news.ycombinator.com/item?id=1",
                text, 0xFF000000, level, false, null, null);
        HNCommentTreeNode node = new HNCommentTreeNode(comment);
        comment.setTreeNode(node);
        return comment;
    }
}
