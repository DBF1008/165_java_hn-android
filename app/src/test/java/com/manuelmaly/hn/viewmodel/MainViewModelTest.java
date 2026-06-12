package com.manuelmaly.hn.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.concurrent.AppExecutors;
import com.manuelmaly.hn.data.FeedRepository;
import com.manuelmaly.hn.data.HNApiException;
import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.testutil.FakeLocalCache;
import com.manuelmaly.hn.testutil.FakeReadStateStore;
import com.manuelmaly.hn.testutil.FakeRemoteDataSource;
import com.manuelmaly.hn.testutil.ManualExecutor;
import com.manuelmaly.hn.testutil.Models;
import com.manuelmaly.hn.testutil.RecordingObserver;
import com.manuelmaly.hn.testutil.SyncExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link MainViewModel} covering the two regression scenarios called
 * out by the task: <b>data update</b> (load / load-more / vote / error / cache) and
 * <b>configuration change</b> (ViewModel retention + LiveData replay). All pure-JVM
 * via {@link InstantTaskExecutorRule} + fakes; no Android framework or Robolectric.
 */
public class MainViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    private FakeRemoteDataSource remote;
    private FakeLocalCache cache;
    private FakeReadStateStore readStore;
    private MainViewModel viewModel;

    @Before
    public void setUp() {
        remote = new FakeRemoteDataSource();
        cache = new FakeLocalCache();
        readStore = new FakeReadStateStore();
        viewModel = newViewModel(new SyncExecutors());
    }

    private MainViewModel newViewModel(AppExecutors executors) {
        FeedRepository repository = new FeedRepository(remote, cache, executors);
        return new MainViewModel(repository, readStore, executors);
    }

    // --- Data update ------------------------------------------------------

    @Test
    public void loadFeed_emitsLoadingThenSuccessWithData() {
        remote.feedResult = Models.feedWith(Models.post("1"));
        RecordingObserver<Resource<HNFeed>> observer = new RecordingObserver<Resource<HNFeed>>();
        viewModel.getFeed().observeForever(observer);
        int start = observer.size();

        viewModel.loadFeed();

        List<Resource<HNFeed>> emitted = observer.values.subList(start, observer.size());
        assertEquals(2, emitted.size());
        assertEquals(Resource.Status.LOADING, emitted.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, emitted.get(1).getStatus());
        assertEquals(1, emitted.get(1).getData().getPosts().size());
        // The successful feed was written to the cache.
        assertSame(remote.feedResult, cache.savedFeed);
    }

    @Test
    public void loadFeed_error_emitsError_andKeepsPreviousData() {
        remote.feedResult = Models.feedWith(Models.post("1"));
        viewModel.loadFeed(); // seed a successful feed

        remote.feedError = new HNApiException(IAPICommand.ERROR_DEVICE_OFFLINE);
        RecordingObserver<Integer> errors = new RecordingObserver<Integer>();
        viewModel.getErrorMessage().observeForever(errors);

        viewModel.loadFeed();

        Resource<HNFeed> last = viewModel.getFeed().getValue();
        assertNotNull(last);
        assertEquals(Resource.Status.ERROR, last.getStatus());
        assertEquals(IAPICommand.ERROR_DEVICE_OFFLINE, last.getErrorCode());
        // Previous feed is retained so the list keeps showing content.
        assertEquals(1, last.getData().getPosts().size());
        assertTrue(errors.values.contains(R.string.error_unable_to_retrieve_feed));
    }

    @Test
    public void loadMore_appendsAndDedups_andUpdatesNextPageUrl() {
        remote.feedResult = Models.feed(Models.posts("A", "B"), "page2");
        viewModel.loadFeed();

        remote.moreResult = Models.feed(Models.posts("B", "C"), "page3"); // B is a duplicate
        viewModel.loadMore();

        HNFeed current = viewModel.currentFeed();
        assertEquals(3, current.getPosts().size());
        assertEquals("page3", current.getNextPageURL());
        assertFalse(current.isLoadedMore());
    }

    @Test
    public void loadMore_emptyResult_setsLoadedMoreTrue_andToasts() {
        remote.feedResult = Models.feed(Models.posts("A"), "page2");
        viewModel.loadFeed();

        remote.moreResult = Models.feed(Models.posts(), null); // empty page
        RecordingObserver<Integer> errors = new RecordingObserver<Integer>();
        viewModel.getErrorMessage().observeForever(errors);

        viewModel.loadMore();

        assertTrue(viewModel.currentFeed().isLoadedMore());
        assertTrue(errors.values.contains(R.string.error_unable_to_load_more));
    }

    @Test
    public void loadMore_inFlight_trueDuringCall_falseAfterSuccess() {
        ManualExecutor manual = new ManualExecutor();
        MainViewModel vm = newViewModel(manual);
        remote.feedResult = Models.feed(Models.posts("A"), "page2");
        vm.loadFeed();
        manual.drain(); // complete the initial load

        remote.moreResult = Models.feed(Models.posts("B"), "page3");
        vm.loadMore();
        assertTrue("in-flight should be true after dispatch",
                Boolean.TRUE.equals(vm.getLoadMoreInFlight().getValue()));

        manual.drain();
        assertFalse("in-flight should be false after completion",
                Boolean.TRUE.equals(vm.getLoadMoreInFlight().getValue()));
    }

    @Test
    public void loadMore_inFlight_falseAfterError() {
        ManualExecutor manual = new ManualExecutor();
        MainViewModel vm = newViewModel(manual);
        remote.feedResult = Models.feed(Models.posts("A"), "page2");
        vm.loadFeed();
        manual.drain();

        remote.moreError = new HNApiException(IAPICommand.ERROR_DEVICE_OFFLINE);
        vm.loadMore();
        assertTrue(Boolean.TRUE.equals(vm.getLoadMoreInFlight().getValue()));

        manual.drain();
        assertFalse(Boolean.TRUE.equals(vm.getLoadMoreInFlight().getValue()));
    }

    @Test
    public void vote_success_addsToUpvotedSet_emitsTrue() {
        remote.voteResult = true;
        HNPost post = Models.post("A");
        RecordingObserver<Boolean> votes = new RecordingObserver<Boolean>();
        viewModel.getVoteEvent().observeForever(votes);

        viewModel.vote("http://vote", post);

        assertTrue(viewModel.isUpvoted(post));
        assertTrue(votes.values.contains(Boolean.TRUE));
        assertEquals("http://vote", remote.lastVoteUrl);
    }

    @Test
    public void vote_rejected_doesNotAddToUpvotedSet_emitsFalse() {
        remote.voteResult = false; // server rejected (e.g. not logged in)
        HNPost post = Models.post("A");
        RecordingObserver<Boolean> votes = new RecordingObserver<Boolean>();
        viewModel.getVoteEvent().observeForever(votes);

        viewModel.vote("http://vote", post);

        assertFalse(viewModel.isUpvoted(post));
        assertTrue(votes.values.contains(Boolean.FALSE));
    }

    @Test
    public void vote_networkError_emitsFalse() {
        remote.voteError = new HNApiException(IAPICommand.ERROR_GENERIC_COMMUNICATION_ERROR);
        HNPost post = Models.post("A");
        RecordingObserver<Boolean> votes = new RecordingObserver<Boolean>();
        viewModel.getVoteEvent().observeForever(votes);

        viewModel.vote("http://vote", post);

        assertFalse(viewModel.isUpvoted(post));
        assertTrue(votes.values.contains(Boolean.FALSE));
    }

    @Test
    public void markRead_addsToAlreadyRead_andPersists() {
        HNPost post = Models.post("A");
        assertFalse(viewModel.isRead(post));

        viewModel.markRead(post);

        assertTrue(viewModel.isRead(post));
        assertTrue(readStore.marked.contains(post.getTitle()));
    }

    @Test
    public void loadCached_thenNetwork_emitsCachedThenFresh() {
        cache.cachedFeed = Models.feed(Models.posts("CACHED"), "p", "alice");
        viewModel.loadCached("alice");
        assertEquals(1, viewModel.currentFeed().getPosts().size());
        assertEquals("CACHED", viewModel.currentFeed().getPosts().get(0).getPostID());

        remote.feedResult = Models.feed(Models.posts("F1", "F2"), "p2", "alice");
        viewModel.loadFeed();
        assertEquals(2, viewModel.currentFeed().getPosts().size());
    }

    @Test
    public void loadCached_userMismatch_doesNotShow() {
        cache.cachedFeed = Models.feed(Models.posts("CACHED"), "p", "bob");

        viewModel.loadCached("alice");

        assertTrue(viewModel.currentFeed().getPosts().isEmpty());
    }

    // --- Configuration change ---------------------------------------------

    @Test
    public void viewModel_retainedAcrossRecreation_sameInstanceAndState() {
        // A ViewModelStore survives Activity recreation; getting the VM twice from
        // the same store must return the same instance with its state intact.
        final AppExecutors executors = new SyncExecutors();
        final FeedRepository repository = new FeedRepository(remote, cache, executors);
        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends ViewModel> T create(Class<T> modelClass) {
                return (T) new MainViewModel(repository, readStore, executors);
            }
        };

        ViewModelStore store = new ViewModelStore();
        MainViewModel first = new ViewModelProvider(store, factory).get(MainViewModel.class);

        remote.feedResult = Models.feedWith(Models.post("A"));
        first.loadFeed();

        // Simulate recreation: same store, fresh provider.
        MainViewModel second = new ViewModelProvider(store, factory).get(MainViewModel.class);

        assertSame(first, second);
        assertNotNull(second.getFeed().getValue());
        assertEquals(Resource.Status.SUCCESS, second.getFeed().getValue().getStatus());
        assertEquals(1, second.getFeed().getValue().getData().getPosts().size());
    }

    @Test
    public void liveData_replaysLastValueToNewObserver() {
        remote.feedResult = Models.feedWith(Models.post("A"));
        viewModel.loadFeed();

        // A newly attached observer (e.g. the recreated Activity) immediately gets
        // the last value, so the UI re-renders without a reload.
        RecordingObserver<Resource<HNFeed>> reattached = new RecordingObserver<Resource<HNFeed>>();
        viewModel.getFeed().observeForever(reattached);

        assertEquals(Resource.Status.SUCCESS, reattached.values.get(0).getStatus());
        assertEquals(1, reattached.values.get(0).getData().getPosts().size());
    }
}
