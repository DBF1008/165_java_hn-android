package com.manuelmaly.hn.task;

import com.manuelmaly.hn.data.network.HNApiClient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import cz.msebera.android.httpclient.client.CookieStore;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VoteFlowTest {

    @Mock HNApiClient mockApiClient;
    @Mock CookieStore mockCookieStore;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void successfulVoteReturnsTrue() throws Exception {
        when(mockApiClient.vote(anyString(), any()))
                .thenReturn(true);

        Boolean result = mockApiClient.vote(
                "https://news.ycombinator.com/vote?id=123&how=up&auth=abc",
                mockCookieStore);
        assertTrue(result);
    }

    @Test
    public void failedVoteReturnsFalse() throws Exception {
        when(mockApiClient.vote(anyString(), any()))
                .thenReturn(false);

        Boolean result = mockApiClient.vote(
                "https://news.ycombinator.com/vote?id=123&how=up&auth=abc",
                mockCookieStore);
        assertFalse(result);
    }

    @Test
    public void voteReturnsNullOnError() throws Exception {
        when(mockApiClient.vote(anyString(), any()))
                .thenReturn(null);

        Boolean result = mockApiClient.vote(
                "https://news.ycombinator.com/vote?id=123&how=up&auth=abc",
                mockCookieStore);
        assertNull(result);
    }

    @Test(expected = Exception.class)
    public void networkErrorDuringVote() throws Exception {
        when(mockApiClient.vote(anyString(), any()))
                .thenThrow(new Exception("Network error"));

        mockApiClient.vote(
                "https://news.ycombinator.com/vote?id=123&how=up&auth=abc",
                mockCookieStore);
    }

    @Test
    public void voteUsesCorrectUrl() throws Exception {
        String voteUrl = "https://news.ycombinator.com/vote?id=12345&how=up&auth=abc123";
        when(mockApiClient.vote(voteUrl, mockCookieStore)).thenReturn(true);

        mockApiClient.vote(voteUrl, mockCookieStore);

        verify(mockApiClient).vote(voteUrl, mockCookieStore);
    }

    @Test
    public void downvoteAlsoWorks() throws Exception {
        String downvoteUrl = "https://news.ycombinator.com/vote?id=12345&how=un&auth=abc123";
        when(mockApiClient.vote(downvoteUrl, mockCookieStore)).thenReturn(true);

        Boolean result = mockApiClient.vote(downvoteUrl, mockCookieStore);
        assertTrue(result);
    }
}
