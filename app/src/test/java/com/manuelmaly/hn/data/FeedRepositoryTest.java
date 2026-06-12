package com.manuelmaly.hn.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.testutil.FakeLocalCache;
import com.manuelmaly.hn.testutil.FakeRemoteDataSource;
import com.manuelmaly.hn.testutil.Models;
import com.manuelmaly.hn.testutil.SyncExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link FeedRepository}: it should emit LOADING then SUCCESS/ERROR,
 * forward the previous feed for paging, write successful loads to the cache, and map
 * the data source's outcome onto {@link Resource}.
 */
public class FeedRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    private FakeRemoteDataSource remote;
    private FakeLocalCache cache;
    private FeedRepository repository;

    @Before
    public void setUp() {
        remote = new FakeRemoteDataSource();
        cache = new FakeLocalCache();
        repository = new FeedRepository(remote, cache, new SyncExecutors());
    }

    private static <T> RepoCallback<T> collectInto(final List<Resource<T>> sink) {
        return new RepoCallback<T>() {
            @Override
            public void onResult(Resource<T> resource) {
                sink.add(resource);
            }
        };
    }

    @Test
    public void loadFeed_emitsLoadingThenSuccess_andCaches() {
        remote.feedResult = Models.feedWith(Models.post("A"));
        List<Resource<HNFeed>> results = new ArrayList<Resource<HNFeed>>();

        repository.loadFeed(null, collectInto(results));

        assertEquals(2, results.size());
        assertEquals(Resource.Status.LOADING, results.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, results.get(1).getStatus());
        assertEquals(1, results.get(1).getData().getPosts().size());
        assertSame(remote.feedResult, cache.savedFeed);
    }

    @Test
    public void loadFeed_apiException_deliversError_withPreviousData() {
        HNFeed previous = Models.feedWith(Models.post("OLD"));
        remote.feedError = new HNApiException(IAPICommand.ERROR_SERVER_RETURNED_ERROR);
        List<Resource<HNFeed>> results = new ArrayList<Resource<HNFeed>>();

        repository.loadFeed(previous, collectInto(results));

        Resource<HNFeed> last = results.get(results.size() - 1);
        assertEquals(Resource.Status.ERROR, last.getStatus());
        assertEquals(IAPICommand.ERROR_SERVER_RETURNED_ERROR, last.getErrorCode());
        assertSame(previous, last.getData());
    }

    @Test
    public void loadMore_passesPreviousFeedForPaging() {
        HNFeed previous = Models.feed(Models.posts("A"), "page2");
        remote.moreResult = Models.feed(Models.posts("B"), "page3");
        List<Resource<HNFeed>> results = new ArrayList<Resource<HNFeed>>();

        repository.loadMore(previous, collectInto(results));

        assertSame(previous, remote.lastFetchMoreArg);
        assertEquals("page2", remote.lastFetchMoreArg.getNextPageURL());
        assertEquals(Resource.Status.SUCCESS, results.get(results.size() - 1).getStatus());
    }

    @Test
    public void vote_success_mapsToBooleanResource() {
        remote.voteResult = true;
        List<Resource<Boolean>> results = new ArrayList<Resource<Boolean>>();

        repository.vote("http://vote", FeedRepositoryTest.<Boolean>collectInto(results));

        Resource<Boolean> last = results.get(results.size() - 1);
        assertEquals(Resource.Status.SUCCESS, last.getStatus());
        assertEquals(Boolean.TRUE, last.getData());
    }

    @Test
    public void vote_networkError_deliversErrorWithFalse() {
        remote.voteError = new HNApiException(IAPICommand.ERROR_GENERIC_COMMUNICATION_ERROR);
        List<Resource<Boolean>> results = new ArrayList<Resource<Boolean>>();

        repository.vote("http://vote", FeedRepositoryTest.<Boolean>collectInto(results));

        Resource<Boolean> last = results.get(results.size() - 1);
        assertEquals(Resource.Status.ERROR, last.getStatus());
        assertEquals(Boolean.FALSE, last.getData());
    }

    @Test
    public void loadCached_returnsCachedFeedAsSuccess() {
        cache.cachedFeed = Models.feedWith(Models.post("CACHED"));
        List<Resource<HNFeed>> results = new ArrayList<Resource<HNFeed>>();

        repository.loadCached(collectInto(results));

        Resource<HNFeed> last = results.get(results.size() - 1);
        assertEquals(Resource.Status.SUCCESS, last.getStatus());
        assertSame(cache.cachedFeed, last.getData());
    }
}
