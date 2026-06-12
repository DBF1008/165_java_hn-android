package com.manuelmaly.hn.reuse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.manuelmaly.hn.reuse.SwipeNavigator.Direction;
import com.manuelmaly.hn.reuse.SwipeNavigator.Page;

import org.junit.Test;

/**
 * Regression tests for the swipe-navigation logic ({@link SwipeNavigator}).
 *
 * Coverage:
 * <ul>
 *   <li><b>Normal switching</b> - the article/comments/list transitions a swipe should produce.</li>
 *   <li><b>Edge cases</b> - boundaries (nothing to navigate to) and gestures that are too short /
 *       too slow / too vertical to count.</li>
 *   <li><b>Compatibility</b> - vertical gestures are never treated as swipes (so list scrolling and
 *       pull-to-refresh are unaffected), and swipe destinations match the existing navigation paths.</li>
 * </ul>
 *
 * These tests are deliberately free of Android dependencies so they run on a plain JVM.
 */
public class SwipeNavigatorTest {

    // Representative thresholds. At runtime these are density-scaled from dp by
    // SwipeNavigationController; fixed pixel values keep the logic tests deterministic.
    private static final float MIN_DISTANCE = 80f;
    private static final float MIN_VELOCITY = 200f;

    /** A clean right-to-left fling (content moves left) => FORWARD. */
    private static Direction swipeLeft() {
        return SwipeNavigator.classifySwipe(-200f, 0f, -1000f, MIN_DISTANCE, MIN_VELOCITY);
    }

    /** A clean left-to-right fling (content moves right) => BACKWARD. */
    private static Direction swipeRight() {
        return SwipeNavigator.classifySwipe(200f, 0f, 1000f, MIN_DISTANCE, MIN_VELOCITY);
    }

    // ---- Normal switching --------------------------------------------------------------------

    @Test
    public void articleForwardGoesToComments() {
        assertEquals(Page.COMMENTS, SwipeNavigator.target(Page.ARTICLE, Direction.FORWARD));
    }

    @Test
    public void articleBackwardGoesToList() {
        assertEquals(Page.LIST, SwipeNavigator.target(Page.ARTICLE, Direction.BACKWARD));
    }

    @Test
    public void commentsBackwardGoesToArticle() {
        assertEquals(Page.ARTICLE, SwipeNavigator.target(Page.COMMENTS, Direction.BACKWARD));
    }

    @Test
    public void cleanLeftSwipeIsForward() {
        assertEquals(Direction.FORWARD, swipeLeft());
    }

    @Test
    public void cleanRightSwipeIsBackward() {
        assertEquals(Direction.BACKWARD, swipeRight());
    }

    /** End-to-end: a left swipe on the reader resolves all the way to the comments page. */
    @Test
    public void swipeLeftFromArticleResolvesToComments() {
        assertEquals(Page.COMMENTS, SwipeNavigator.target(Page.ARTICLE, swipeLeft()));
    }

    /** End-to-end: a right swipe on comments resolves all the way back to the reader. */
    @Test
    public void swipeRightFromCommentsResolvesToArticle() {
        assertEquals(Page.ARTICLE, SwipeNavigator.target(Page.COMMENTS, swipeRight()));
    }

    // ---- Edge cases: boundaries --------------------------------------------------------------

    @Test
    public void listHasNoBackwardTarget() {
        assertNull(SwipeNavigator.target(Page.LIST, Direction.BACKWARD));
    }

    @Test
    public void listHasNoForwardTarget() {
        // Nothing is selected while viewing the list, so there is no article to swipe into.
        assertNull(SwipeNavigator.target(Page.LIST, Direction.FORWARD));
    }

    @Test
    public void commentsHasNoForwardTarget() {
        // Comments is the right-most page.
        assertNull(SwipeNavigator.target(Page.COMMENTS, Direction.FORWARD));
    }

    @Test
    public void nullInputsYieldNoTarget() {
        assertNull(SwipeNavigator.target(null, Direction.FORWARD));
        assertNull(SwipeNavigator.target(Page.ARTICLE, null));
    }

    // ---- Edge cases: gesture recognition -----------------------------------------------------

    @Test
    public void tooShortHorizontalIsIgnored() {
        assertNull(SwipeNavigator.classifySwipe(40f, 0f, 1000f, MIN_DISTANCE, MIN_VELOCITY));
    }

    @Test
    public void tooSlowIsIgnored() {
        assertNull(SwipeNavigator.classifySwipe(200f, 0f, 50f, MIN_DISTANCE, MIN_VELOCITY));
    }

    @Test
    public void distanceExactlyAtThresholdCounts() {
        assertEquals(Direction.BACKWARD, SwipeNavigator.classifySwipe(
                MIN_DISTANCE, 0f, MIN_VELOCITY, MIN_DISTANCE, MIN_VELOCITY));
    }

    @Test
    public void velocityJustBelowThresholdIsIgnored() {
        assertNull(SwipeNavigator.classifySwipe(
                200f, 0f, MIN_VELOCITY - 1f, MIN_DISTANCE, MIN_VELOCITY));
    }

    // ---- Compatibility: vertical gestures must pass through to existing handlers --------------

    @Test
    public void purelyVerticalGestureIsIgnored() {
        // Protects ListView vertical scrolling.
        assertNull(SwipeNavigator.classifySwipe(0f, 300f, 0f, MIN_DISTANCE, MIN_VELOCITY));
    }

    @Test
    public void verticalDominantDiagonalIsIgnored() {
        // Protects SwipeRefreshLayout pull-to-refresh even on slightly diagonal pulls.
        assertNull(SwipeNavigator.classifySwipe(90f, 150f, 1000f, MIN_DISTANCE, MIN_VELOCITY));
    }

    @Test
    public void equalHorizontalAndVerticalIsIgnored() {
        assertNull(SwipeNavigator.classifySwipe(100f, 100f, 1000f, MIN_DISTANCE, MIN_VELOCITY));
    }

    @Test
    public void horizontalDominantDiagonalStillSwipes() {
        assertEquals(Direction.FORWARD, SwipeNavigator.classifySwipe(
                -200f, 90f, -1000f, MIN_DISTANCE, MIN_VELOCITY));
    }

    // ---- Compatibility: swipe destinations match the existing navigation paths ----------------
    // article BACKWARD == back-to-list (finish());
    // article FORWARD  == ArticleReaderActivity.launchCommentsActivity();
    // comments BACKWARD == CommentsActivity.openArticleReader().

    @Test
    public void swipeTargetsMatchExistingNavigationPaths() {
        assertEquals(Page.LIST, SwipeNavigator.target(Page.ARTICLE, Direction.BACKWARD));
        assertEquals(Page.COMMENTS, SwipeNavigator.target(Page.ARTICLE, Direction.FORWARD));
        assertEquals(Page.ARTICLE, SwipeNavigator.target(Page.COMMENTS, Direction.BACKWARD));
    }
}
