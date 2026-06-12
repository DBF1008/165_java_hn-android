package com.manuelmaly.hn.feed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link FeedType} — the feed-category configuration (URL/path/id/title mapping).
 * Pure JVM tests; no Android dependency.
 */
public class FeedTypeTest {

    @Test
    public void fromId_knownId_returnsType() {
        assertSame(FeedType.ASK, FeedType.fromId("ask", FeedType.TOP));
        assertSame(FeedType.JOBS, FeedType.fromId("jobs", FeedType.TOP));
    }

    @Test
    public void fromId_unknownId_returnsDefaultArg() {
        assertSame(FeedType.NEW, FeedType.fromId("does-not-exist", FeedType.NEW));
    }

    @Test
    public void fromId_nullId_returnsDefaultArg() {
        assertSame(FeedType.TOP, FeedType.fromId(null, FeedType.TOP));
    }

    @Test
    public void getFeedURL_appendsPath() {
        assertEquals("https://news.ycombinator.com/news", FeedType.TOP.getFeedURL());
        assertEquals("https://news.ycombinator.com/newest", FeedType.NEW.getFeedURL());
        assertEquals("https://news.ycombinator.com/ask", FeedType.ASK.getFeedURL());
        assertEquals("https://news.ycombinator.com/show", FeedType.SHOW.getFeedURL());
        assertEquals("https://news.ycombinator.com/jobs", FeedType.JOBS.getFeedURL());
    }

    @Test
    public void cacheFileName_isPerId() {
        assertEquals("lastHNFeed_top", FeedType.TOP.cacheFileName());
        assertEquals("lastHNFeed_jobs", FeedType.JOBS.cacheFileName());
    }

    @Test
    public void getDefault_isTop() {
        assertSame(FeedType.TOP, FeedType.getDefault());
    }

    @Test
    public void ordered_isTopNewAskShowJobs() {
        List<FeedType> order = FeedType.ordered();
        assertEquals(5, order.size());
        assertSame(FeedType.TOP, order.get(0));
        assertSame(FeedType.NEW, order.get(1));
        assertSame(FeedType.ASK, order.get(2));
        assertSame(FeedType.SHOW, order.get(3));
        assertSame(FeedType.JOBS, order.get(4));
    }
}
