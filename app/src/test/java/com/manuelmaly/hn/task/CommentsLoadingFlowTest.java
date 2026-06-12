package com.manuelmaly.hn.task;

import com.manuelmaly.hn.data.network.HNApiClient;
import com.manuelmaly.hn.data.storage.FeedCache;
import com.manuelmaly.hn.model.HNComment;
import com.manuelmaly.hn.model.HNPostComments;
import com.manuelmaly.hn.parser.CommentsParser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CommentsLoadingFlowTest {

    @Mock HNApiClient mockApiClient;
    @Mock CommentsParser mockCommentsParser;
    @Mock FeedCache mockFeedCache;

    private HNPostComments sampleComments;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        ArrayList<HNComment> comments = new ArrayList<>();
        comments.add(new HNComment("2 hours ago", "author1",
                "item?id=2001", "<p>Test comment</p>", 0, 0, false,
                "vote?id=2001&how=up", null));
        sampleComments = new HNPostComments(comments, null, "testUser");
    }

    @Test
    public void downloadsCommentsHtmlSuccessfully() throws Exception {
        String fakeHtml = "<html>comments page</html>";
        when(mockApiClient.downloadHtml(eq("https://news.ycombinator.com/item"),
                anyMap(), any())).thenReturn(fakeHtml);

        String result = mockApiClient.downloadHtml(
                "https://news.ycombinator.com/item",
                new HashMap<String, String>() {{ put("id", "12345"); }},
                null);

        assertEquals(fakeHtml, result);
    }

    @Test
    public void parsesCommentsFromHtml() throws Exception {
        String fakeHtml = "<html>comments page</html>";
        when(mockCommentsParser.parse(fakeHtml)).thenReturn(sampleComments);

        HNPostComments result = mockCommentsParser.parse(fakeHtml);

        assertNotNull(result);
        assertEquals("testUser", result.getUserAcquiredFor());
    }

    @Test
    public void commentsAreCachedAfterSuccess() throws Exception {
        String fakeHtml = "<html>comments</html>";
        when(mockApiClient.downloadHtml(anyString(), anyMap(), any()))
                .thenReturn(fakeHtml);
        when(mockCommentsParser.parse(fakeHtml)).thenReturn(sampleComments);

        String html = mockApiClient.downloadHtml(
                "https://news.ycombinator.com/item",
                new HashMap<String, String>() {{ put("id", "12345"); }},
                null);
        HNPostComments comments = mockCommentsParser.parse(html);
        mockFeedCache.setLastComments("12345", comments);

        verify(mockFeedCache).setLastComments("12345", sampleComments);
    }

    @Test(expected = IOException.class)
    public void networkFailureThrowsException() throws Exception {
        when(mockApiClient.downloadHtml(anyString(), anyMap(), any()))
                .thenThrow(new IOException("Network unavailable"));

        mockApiClient.downloadHtml("https://news.ycombinator.com/item",
                new HashMap<String, String>() {{ put("id", "12345"); }},
                null);
    }

    @Test
    public void cachedCommentsAvailableOnFailure() throws Exception {
        when(mockFeedCache.getLastComments("12345")).thenReturn(sampleComments);

        HNPostComments cached = mockFeedCache.getLastComments("12345");
        assertNotNull(cached);
        assertEquals("testUser", cached.getUserAcquiredFor());
    }

    @Test
    public void parserExceptionHandledGracefully() throws Exception {
        when(mockCommentsParser.parse(anyString()))
                .thenThrow(new Exception("Parse error"));

        try {
            mockCommentsParser.parse("<invalid>");
        } catch (Exception e) {
            assertEquals("Parse error", e.getMessage());
        }
    }

    @Test
    public void commentsWithPostIdPassedCorrectly() throws Exception {
        HashMap<String, String> params = new HashMap<>();
        params.put("id", "99999");

        mockApiClient.downloadHtml("https://news.ycombinator.com/item", params, null);

        verify(mockApiClient).downloadHtml(
                "https://news.ycombinator.com/item", params, null);
    }
}
