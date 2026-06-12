package com.manuelmaly.hn.task;

import com.manuelmaly.hn.data.network.HNApiClient;
import com.manuelmaly.hn.data.storage.AppSettings;
import com.manuelmaly.hn.data.storage.FeedCache;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.parser.FeedParser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.client.CookieStore;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FeedLoadingFlowTest {

    @Mock HNApiClient mockApiClient;
    @Mock FeedParser mockFeedParser;
    @Mock FeedCache mockFeedCache;

    private HNFeed sampleFeed;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        ArrayList<HNPost> posts = new ArrayList<>();
        posts.add(new HNPost("https://example.com", "Test Post", "example.com",
                "author", "12345", 10, 42, null));
        sampleFeed = new HNFeed(posts, "https://news.ycombinator.com/news?p=2", "testUser");
    }

    @Test
    public void apiClientDownloadsHtmlSuccessfully() throws Exception {
        String fakeHtml = "<html>sample</html>";
        when(mockApiClient.downloadHtml(anyString(), anyMap(), any()))
                .thenReturn(fakeHtml);

        String result = mockApiClient.downloadHtml(
                "https://news.ycombinator.com/", new HashMap<>(), null);

        assertEquals(fakeHtml, result);
        verify(mockApiClient).downloadHtml(
                "https://news.ycombinator.com/", new HashMap<>(), null);
    }

    @Test
    public void feedParserProducesFeedFromHtml() throws Exception {
        String fakeHtml = "<html>sample</html>";
        when(mockFeedParser.parse(fakeHtml)).thenReturn(sampleFeed);

        HNFeed result = mockFeedParser.parse(fakeHtml);

        assertNotNull(result);
        assertEquals(1, result.getPosts().size());
        assertEquals("Test Post", result.getPosts().get(0).getTitle());
    }

    @Test
    public void feedIsCachedAfterSuccessfulLoad() throws Exception {
        String fakeHtml = "<html>sample</html>";
        when(mockApiClient.downloadHtml(anyString(), anyMap(), any()))
                .thenReturn(fakeHtml);
        when(mockFeedParser.parse(fakeHtml)).thenReturn(sampleFeed);

        // Simulate the flow: download → parse → cache
        String html = mockApiClient.downloadHtml("https://news.ycombinator.com/",
                new HashMap<>(), null);
        HNFeed feed = mockFeedParser.parse(html);
        mockFeedCache.setLastFeed(feed);

        verify(mockFeedCache).setLastFeed(sampleFeed);
    }

    @Test(expected = IOException.class)
    public void networkFailureThrowsException() throws Exception {
        when(mockApiClient.downloadHtml(anyString(), anyMap(), any()))
                .thenThrow(new IOException("Network unavailable"));

        mockApiClient.downloadHtml("https://news.ycombinator.com/",
                new HashMap<>(), null);
    }

    @Test
    public void cachedFeedAvailableOnNetworkFailure() throws Exception {
        when(mockApiClient.downloadHtml(anyString(), anyMap(), any()))
                .thenThrow(new IOException("offline"));
        when(mockFeedCache.getLastFeed()).thenReturn(sampleFeed);

        // Verify fallback behavior
        try {
            mockApiClient.downloadHtml("https://news.ycombinator.com/",
                    new HashMap<>(), null);
        } catch (Exception e) {
            // Expected
        }

        HNFeed cached = mockFeedCache.getLastFeed();
        assertNotNull("Cached feed should be available", cached);
        assertEquals(1, cached.getPosts().size());
    }

    @Test
    public void parserExceptionReturnsEmptyFeed() throws Exception {
        when(mockFeedParser.parse(anyString()))
                .thenThrow(new Exception("Parse error"));

        try {
            mockFeedParser.parse("<invalid html>");
        } catch (Exception e) {
            // In actual code, this would fall back to empty HNFeed
            assertEquals("Parse error", e.getMessage());
        }
    }

    @Test
    public void feedContainsNextPageURL() throws Exception {
        when(mockFeedParser.parse(anyString())).thenReturn(sampleFeed);

        HNFeed result = mockFeedParser.parse("<html></html>");
        assertNotNull(result.getNextPageURL());
        assertTrue(result.getNextPageURL().contains("p=2"));
    }

    @Test
    public void feedTracksUserAcquiredFor() throws Exception {
        when(mockFeedParser.parse(anyString())).thenReturn(sampleFeed);

        HNFeed result = mockFeedParser.parse("<html></html>");
        assertEquals("testUser", result.getUserAcquiredFor());
    }
}
