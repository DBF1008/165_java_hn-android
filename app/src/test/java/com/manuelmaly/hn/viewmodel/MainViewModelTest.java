package com.manuelmaly.hn.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.manuelmaly.hn.data.HNFeedRepository;
import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for {@link MainViewModel}.
 * Verifies data update flow, state management, and configuration change resilience.
 */
public class MainViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private Application application;
    private HNFeedRepository mockRepository;
    private MainViewModel viewModel;

    private MutableLiveData<Resource<HNFeed>> feedLiveData;
    private MutableLiveData<Resource<HNFeed>> loadMoreLiveData;
    private MutableLiveData<Set<Integer>> alreadyReadLiveData;

    @Before
    public void setUp() {
        application = mock(Application.class);
        mockRepository = mock(HNFeedRepository.class);

        feedLiveData = new MutableLiveData<>();
        loadMoreLiveData = new MutableLiveData<>();
        alreadyReadLiveData = new MutableLiveData<>(new HashSet<Integer>());

        when(mockRepository.getFeed()).thenReturn(feedLiveData);
        when(mockRepository.getLoadMoreResult()).thenReturn(loadMoreLiveData);
        when(mockRepository.getAlreadyRead()).thenReturn(alreadyReadLiveData);

        viewModel = new MainViewModel(application, mockRepository);
    }

    @Test
    public void refresh_delegatesToRepository() {
        viewModel.refresh();
        verify(mockRepository).refresh();
    }

    @Test
    public void loadMore_delegatesToRepository() {
        HNFeed feed = createTestFeed("Post 1", "Post 2");
        viewModel.updateCurrentFeed(feed);

        viewModel.loadMore();

        ArgumentCaptor<HNFeed> captor = ArgumentCaptor.forClass(HNFeed.class);
        verify(mockRepository).loadMore(captor.capture());
        assertEquals(2, captor.getValue().getPosts().size());
    }

    @Test
    public void feedLoading_emitsLoadingThenSuccess() {
        // Observe the feed status
        List<Resource<HNFeed>> observed = new ArrayList<>();
        viewModel.getFeedStatus().observeForever(resource -> observed.add(resource));

        // Simulate loading
        feedLiveData.postValue(Resource.loading(null));
        assertEquals(1, observed.size());
        assertEquals(Resource.Status.LOADING, observed.get(0).status);

        // Simulate success
        HNFeed feed = createTestFeed("Test Post");
        feedLiveData.postValue(Resource.success(feed));
        assertEquals(2, observed.size());
        assertEquals(Resource.Status.SUCCESS, observed.get(1).status);
        assertEquals(1, observed.get(1).data.getPosts().size());
    }

    @Test
    public void feedLoading_emitsErrorOnFailure() {
        List<Resource<HNFeed>> observed = new ArrayList<>();
        viewModel.getFeedStatus().observeForever(resource -> observed.add(resource));

        feedLiveData.postValue(Resource.error("Network error", null));
        assertEquals(1, observed.size());
        assertEquals(Resource.Status.ERROR, observed.get(0).status);
        assertEquals("Network error", observed.get(0).message);
    }

    @Test
    public void markAsRead_delegatesToRepository() {
        HNPost post = createTestPost("Test Article");
        viewModel.markAsRead(post);
        verify(mockRepository).markAsRead(post);
    }

    @Test
    public void recordUpvote_persistsAcrossObservation() {
        // Simulate a successful upvote
        HNPost post = createTestPost("Upvoted Post");
        viewModel.recordUpvote(post);

        // Verify upvoted posts LiveData is updated
        Set<HNPost> upvoted = viewModel.getUpvotedPosts().getValue();
        assertNotNull(upvoted);
        assertTrue(upvoted.contains(post));

        // Simulate re-observation (as would happen after configuration change)
        List<Set<HNPost>> observed = new ArrayList<>();
        viewModel.getUpvotedPosts().observeForever(voted -> observed.add(voted));

        // The latest value should still contain the upvoted post
        Set<HNPost> latest = observed.get(observed.size() - 1);
        assertTrue(latest.contains(post));
    }

    @Test
    public void postList_rebuildsWhenFeedChanges() {
        List<List<PostUiModel>> observed = new ArrayList<>();
        viewModel.getPostList().observeForever(posts -> observed.add(posts));

        // Push a feed
        HNFeed feed = createTestFeed("Post A", "Post B", "Post C");
        feedLiveData.postValue(Resource.success(feed));

        List<PostUiModel> latest = observed.get(observed.size() - 1);
        assertEquals(3, latest.size());
        assertEquals("Post A", latest.get(0).post.getTitle());
        assertFalse(latest.get(0).isRead);
        assertFalse(latest.get(0).isUpvoted);
    }

    @Test
    public void postList_reflectsReadState() {
        List<List<PostUiModel>> observed = new ArrayList<>();
        viewModel.getPostList().observeForever(posts -> observed.add(posts));

        // Push a feed
        HNFeed feed = createTestFeed("Read Post", "Unread Post");
        feedLiveData.postValue(Resource.success(feed));

        // Mark first post as read
        Set<Integer> readSet = new HashSet<>();
        readSet.add("Read Post".hashCode());
        alreadyReadLiveData.postValue(readSet);

        List<PostUiModel> latest = observed.get(observed.size() - 1);
        assertEquals(2, latest.size());
        assertTrue(latest.get(0).isRead);
        assertFalse(latest.get(1).isRead);
    }

    @Test
    public void postList_reflectsUpvoteState() {
        List<List<PostUiModel>> observed = new ArrayList<>();
        viewModel.getPostList().observeForever(posts -> observed.add(posts));

        HNFeed feed = createTestFeed("Voted Post", "Unvoted Post");
        feedLiveData.postValue(Resource.success(feed));

        // Record upvote
        HNPost votedPost = feed.getPosts().get(0);
        viewModel.recordUpvote(votedPost);

        List<PostUiModel> latest = observed.get(observed.size() - 1);
        assertTrue(latest.get(0).isUpvoted);
        assertFalse(latest.get(1).isUpvoted);
    }

    @Test
    public void loadAlreadyReadCache_delegatesToRepository() {
        viewModel.loadAlreadyReadCache();
        verify(mockRepository).loadAlreadyReadCache();
    }

    @Test
    public void onCleared_disposesRepository() {
        // Simulate ViewModel being cleared (e.g., Activity finishing)
        viewModel.onCleared();
        verify(mockRepository).dispose();
    }

    // --- Helpers ---

    private HNFeed createTestFeed(String... titles) {
        List<HNPost> posts = new ArrayList<>();
        for (String title : titles) {
            posts.add(createTestPost(title));
        }
        return new HNFeed(posts, null, "testUser");
    }

    private HNPost createTestPost(String title) {
        return new HNPost("https://example.com", title, "example.com",
                "author", "12345", 10, 100, null);
    }
}
