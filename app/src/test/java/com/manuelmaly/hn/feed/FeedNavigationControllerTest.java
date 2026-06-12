package com.manuelmaly.hn.feed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Regression tests for {@link FeedNavigationController}, covering the three scenarios required by the
 * feature: feed-category switching, configuration changes (snapshot save/restore), and offline cache
 * fallback. Pure JVM tests; no Android dependency.
 */
public class FeedNavigationControllerTest {

    private static FeedNavigationController newController() {
        return new FeedNavigationController(FeedType.ordered(), FeedType.getDefault());
    }

    private static HNPost post(String title) {
        return new HNPost("https://example.com/" + title, title, "example.com", "author",
                "id_" + title, 1, 1, null);
    }

    private static HNFeed feed(String acquiredForUser, String... titles) {
        List<HNPost> posts = new ArrayList<HNPost>();
        for (String title : titles) {
            posts.add(post(title));
        }
        return new HNFeed(posts, "https://news.ycombinator.com/?p=2", acquiredForUser);
    }

    private static String firstTitle(FeedSession session) {
        return session.getFeed().getPosts().get(0).getTitle();
    }

    // ----------------------------------------------------------------------
    // Category switching
    // ----------------------------------------------------------------------

    @Test
    public void switchTo_preservesPerFeedPostsIndependently() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, true, true, null, feed(null, "t1", "t2"), null);
        c.switchTo(FeedType.NEW, null);
        c.onLoaded(FeedType.NEW, true, true, null, feed(null, "n1"), null);

        assertEquals(2, c.session(FeedType.TOP).getFeed().getPosts().size());
        assertEquals(1, c.session(FeedType.NEW).getFeed().getPosts().size());
        assertEquals("t1", firstTitle(c.session(FeedType.TOP)));
        assertEquals("n1", firstTitle(c.session(FeedType.NEW)));
    }

    @Test
    public void switchTo_savesOutgoingScroll_restoredOnReturn() {
        FeedNavigationController c = newController(); // current = TOP
        c.switchTo(FeedType.NEW, "TOP_SCROLL");       // leaving TOP -> stash its scroll
        assertEquals("TOP_SCROLL", c.session(FeedType.TOP).getScrollState());

        c.switchTo(FeedType.TOP, "NEW_SCROLL");       // leaving NEW -> stash its scroll, back to TOP
        assertEquals("NEW_SCROLL", c.session(FeedType.NEW).getScrollState());
        assertSame(FeedType.TOP, c.current());
        assertEquals("TOP_SCROLL", c.currentSession().getScrollState());
    }

    @Test
    public void switchTo_lazilyCreatesEmptySessionForUnvisited() {
        FeedNavigationController c = newController();
        assertTrue(c.session(FeedType.JOBS).isEmpty());
    }

    @Test
    public void switchTo_doesNotResetEverLoaded() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, true, true, null, feed(null, "t1"), null);
        assertTrue(c.session(FeedType.TOP).isEverLoaded());

        c.switchTo(FeedType.NEW, "scroll");
        c.switchTo(FeedType.TOP, null);
        assertTrue(c.session(FeedType.TOP).isEverLoaded());
    }

    // ----------------------------------------------------------------------
    // Configuration change / process death (snapshot round-trip via real serialization)
    // ----------------------------------------------------------------------

    @Test
    public void saveRestore_roundTripsCurrentAndPerFeedState() throws Exception {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, true, true, null, feed(null, "t1", "t2"), null);
        c.switchTo(FeedType.NEW, "TOP_SCROLL");
        c.onLoaded(FeedType.NEW, true, true, null, feed(null, "n1"), null);
        c.currentSession().setScrollState("NEW_SCROLL"); // current = NEW

        FeedNavSnapshot snapshot = serializeRoundTrip(c.save());

        FeedNavigationController restored = newController();
        restored.restore(snapshot);

        assertSame(FeedType.NEW, restored.current());
        assertEquals(2, restored.session(FeedType.TOP).getFeed().getPosts().size());
        assertEquals(1, restored.session(FeedType.NEW).getFeed().getPosts().size());
        assertEquals("TOP_SCROLL", restored.session(FeedType.TOP).getScrollState());
        assertEquals("NEW_SCROLL", restored.session(FeedType.NEW).getScrollState());
    }

    @Test
    public void saveRestore_emptyControllerIsStable() throws Exception {
        FeedNavigationController c = newController();
        FeedNavSnapshot snapshot = serializeRoundTrip(c.save());

        FeedNavigationController restored = newController();
        restored.restore(snapshot);

        assertSame(FeedType.getDefault(), restored.current());
        for (FeedType type : FeedType.ordered()) {
            assertTrue(restored.session(type).isEmpty());
        }
    }

    private static FeedNavSnapshot serializeRoundTrip(FeedNavSnapshot in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(in);
        oos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        return (FeedNavSnapshot) ois.readObject();
    }

    // ----------------------------------------------------------------------
    // Offline fallback (one assertion per decision-table row + user gating + transitions)
    // ----------------------------------------------------------------------

    @Test
    public void onLoaded_successNonEmpty_usesLoaded_clearsFromCache() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, true, true, null, feed(null, "live"), feed(null, "cached"));
        FeedSession s = c.session(FeedType.TOP);
        assertFalse(s.isFromCache());
        assertTrue(s.isEverLoaded());
        assertEquals("live", firstTitle(s));
    }

    @Test
    public void onLoaded_successEmpty_withExistingPosts_keepsExisting() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, true, true, null, feed(null, "a"), null);
        c.onLoaded(FeedType.TOP, true, false, null, new HNFeed(), null);
        assertEquals(1, c.session(FeedType.TOP).getFeed().getPosts().size());
        assertEquals("a", firstTitle(c.session(FeedType.TOP)));
    }

    @Test
    public void onLoaded_successEmpty_noExisting_withGatedCache_showsCache() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, true, false, null, new HNFeed(), feed(null, "cached"));
        FeedSession s = c.session(FeedType.TOP);
        assertTrue(s.isFromCache());
        assertEquals("cached", firstTitle(s));
    }

    @Test
    public void onLoaded_successEmpty_noExisting_noCache_isEmpty() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, true, false, null, new HNFeed(), null);
        assertTrue(c.session(FeedType.TOP).isEmpty());
    }

    @Test
    public void onLoaded_failure_withExistingPosts_keepsExisting() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, true, true, null, feed(null, "a"), null);
        c.onLoaded(FeedType.TOP, false, false, null, null, feed(null, "cached"));
        FeedSession s = c.session(FeedType.TOP);
        assertEquals(1, s.getFeed().getPosts().size());
        assertEquals("a", firstTitle(s));
        assertFalse(s.isFromCache());
    }

    @Test
    public void onLoaded_failure_noExisting_withGatedCache_showsCache_fromCacheTrue() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, false, false, null, null, feed(null, "cached"));
        FeedSession s = c.session(FeedType.TOP);
        assertTrue(s.isFromCache());
        assertFalse(s.isEverLoaded());
        assertEquals("cached", firstTitle(s));
    }

    @Test
    public void onLoaded_failure_noExisting_noCache_isEmpty() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, false, false, null, null, null);
        assertTrue(c.session(FeedType.TOP).isEmpty());
    }

    @Test
    public void onLoaded_cacheWrongUser_isRejected() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, false, false, "alice", null, feed("bob", "cached"));
        FeedSession s = c.session(FeedType.TOP);
        assertTrue(s.isEmpty());
        assertFalse(s.isFromCache());
    }

    @Test
    public void onLoaded_cacheSameUser_isAccepted() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, false, false, "alice", null, feed("alice", "cached"));
        FeedSession s = c.session(FeedType.TOP);
        assertTrue(s.isFromCache());
        assertEquals("cached", firstTitle(s));
    }

    @Test
    public void onLoaded_successAfterCacheFallback_clearsFromCache_setsEverLoaded() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, false, false, null, null, feed(null, "cached")); // offline -> cache
        assertTrue(c.session(FeedType.TOP).isFromCache());

        c.onLoaded(FeedType.TOP, true, true, null, feed(null, "live"), null);     // back online
        FeedSession s = c.session(FeedType.TOP);
        assertFalse(s.isFromCache());
        assertTrue(s.isEverLoaded());
        assertEquals("live", firstTitle(s));
    }

    @Test
    public void onLoaded_cancelled_treatedAsKeep() {
        FeedNavigationController c = newController();
        c.onLoaded(FeedType.TOP, true, true, null, feed(null, "a"), null);
        // a cancelled load arrives as success=false; existing posts must be preserved
        c.onLoaded(FeedType.TOP, false, false, null, null, null);
        assertEquals(1, c.session(FeedType.TOP).getFeed().getPosts().size());
    }
}
