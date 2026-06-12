package com.manuelmaly.hn.parser;

import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Scanner;

import static org.junit.Assert.*;

public class HNFeedParserTest {

    private HNFeedParser parser;
    private String sampleHtml;

    @Before
    public void setUp() throws Exception {
        parser = new HNFeedParser("testUser");
        sampleHtml = loadResource("sample_feed.html");
    }

    @Test
    public void parsesFeedSuccessfully() throws Exception {
        HNFeed feed = parser.parse(sampleHtml);
        assertNotNull("Feed should not be null", feed);
        assertNotNull("Posts list should not be null", feed.getPosts());
    }

    @Test
    public void parsesCorrectNumberOfPosts() throws Exception {
        HNFeed feed = parser.parse(sampleHtml);
        assertEquals("Should parse 3 posts", 3, feed.getPosts().size());
    }

    @Test
    public void parsesFirstPostCorrectly() throws Exception {
        HNFeed feed = parser.parse(sampleHtml);
        HNPost firstPost = feed.getPosts().get(0);
        assertEquals("First Article Title", firstPost.getTitle());
        assertEquals("https://example.com/article1", firstPost.getURL());
        assertEquals("example.com", firstPost.getURLDomain());
        assertEquals("author1", firstPost.getAuthor());
        assertEquals("1001", firstPost.getPostID());
        assertEquals(42, firstPost.getPoints());
        assertEquals(10, firstPost.getCommentsCount());
    }

    @Test
    public void parsesSecondPostAsAskHN() throws Exception {
        HNFeed feed = parser.parse(sampleHtml);
        HNPost secondPost = feed.getPosts().get(1);
        assertEquals("Ask HN: A Discussion Thread", secondPost.getTitle());
        assertEquals("author2", secondPost.getAuthor());
        assertEquals("1002", secondPost.getPostID());
        assertEquals(100, secondPost.getPoints());
        assertEquals(25, secondPost.getCommentsCount());
    }

    @Test
    public void parsesThirdPostWithZeroComments() throws Exception {
        HNFeed feed = parser.parse(sampleHtml);
        HNPost thirdPost = feed.getPosts().get(2);
        assertEquals("Open Source Project", thirdPost.getTitle());
        assertEquals("github.com", thirdPost.getURLDomain());
        assertEquals(7, thirdPost.getPoints());
        assertEquals(0, thirdPost.getCommentsCount());
    }

    @Test
    public void parsesNextPageURL() throws Exception {
        HNFeed feed = parser.parse(sampleHtml);
        assertNotNull("Next page URL should not be null", feed.getNextPageURL());
        assertTrue("Next page URL should contain p=2",
                feed.getNextPageURL().contains("p=2"));
    }

    @Test
    public void parsesUserAcquiredFor() throws Exception {
        HNFeed feed = parser.parse(sampleHtml);
        assertEquals("testUser", feed.getUserAcquiredFor());
    }

    @Test
    public void handlesNullInput() throws Exception {
        HNFeed feed = parser.parse(null);
        // parse(null) should produce empty feed via Jsoup parsing null
        assertNotNull(feed);
    }

    @Test
    public void handlesEmptyHtml() throws Exception {
        HNFeed feed = parser.parse("<html><body></body></html>");
        assertNotNull(feed);
        assertNotNull(feed.getPosts());
        assertEquals(0, feed.getPosts().size());
    }

    @Test
    public void parsesUpvoteURLs() throws Exception {
        HNFeed feed = parser.parse(sampleHtml);
        HNPost firstPost = feed.getPosts().get(0);
        assertNotNull("Upvote URL should not be null for authenticated user",
                firstPost.getUpvoteURL("testUser"));
        assertTrue("Upvote URL should contain auth=",
                firstPost.getUpvoteURL("testUser").contains("auth="));
    }

    private String loadResource(String name) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull("Resource " + name + " should exist", is);
        Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
        String content = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return content;
    }
}
