package com.manuelmaly.hn.reuse;

/**
 * Pure navigation logic for the left/right swipe gesture feature.
 * <p>
 * This class intentionally has no Android dependencies so it can be exercised by plain JVM unit
 * tests. The Android glue (gesture detection, thresholds, performing the navigation) lives in
 * {@link SwipeNavigationController}.
 * <p>
 * The three primary screens form a horizontal strip:
 *
 * <pre>
 *     LIST  &lt;--&gt;  ARTICLE  &lt;--&gt;  COMMENTS
 * </pre>
 *
 * Swiping the content to the left (finger travels right-to-left) navigates {@link
 * Direction#FORWARD} (towards COMMENTS); swiping the content to the right navigates {@link
 * Direction#BACKWARD} (towards LIST), which matches the platform back gesture.
 */
public final class SwipeNavigator {

    /** The three screens that participate in swipe navigation, ordered left to right. */
    public enum Page {
        LIST, ARTICLE, COMMENTS
    }

    /** Direction of travel along the {@link Page} strip. */
    public enum Direction {
        FORWARD, BACKWARD
    }

    private SwipeNavigator() {
        // Utility class - not instantiable.
    }

    /**
     * Classifies a fling gesture into a navigation {@link Direction}, or {@code null} when the
     * gesture should be ignored. Vertical-dominant, too-short, or too-slow gestures return
     * {@code null} so that vertical list scrolling, WebView panning and pull-to-refresh keep
     * working untouched.
     *
     * @param dx            horizontal travel (end - start), in pixels; positive is left-to-right
     * @param dy            vertical travel (end - start), in pixels
     * @param velocityX     horizontal fling velocity, in pixels per second
     * @param minDistancePx minimum horizontal travel required to count as a swipe
     * @param minVelocityPx minimum horizontal speed required to count as a swipe
     * @return {@link Direction#BACKWARD} for a left-to-right swipe, {@link Direction#FORWARD}
     *         for a right-to-left swipe, or {@code null} if it is not a horizontal navigation swipe
     */
    public static Direction classifySwipe(float dx, float dy, float velocityX,
            float minDistancePx, float minVelocityPx) {
        // Too short to be an intentional swipe.
        if (Math.abs(dx) < minDistancePx) {
            return null;
        }
        // Vertical-dominant (or perfectly diagonal): leave it to the list / WebView /
        // SwipeRefreshLayout so existing vertical gestures are not hijacked.
        if (Math.abs(dx) <= Math.abs(dy)) {
            return null;
        }
        // Too slow to be a deliberate fling (e.g. a leisurely drag-and-release).
        if (Math.abs(velocityX) < minVelocityPx) {
            return null;
        }
        return dx > 0 ? Direction.BACKWARD : Direction.FORWARD;
    }

    /**
     * Resolves the destination page for a swipe, or {@code null} when the swipe hits a boundary
     * and there is nothing to navigate to.
     * <p>
     * Boundary behaviour:
     * <ul>
     *   <li>From {@link Page#LIST}: BACKWARD is the left-most edge; FORWARD has no target because
     *       no article is selected while viewing the list.</li>
     *   <li>From {@link Page#COMMENTS}: FORWARD is the right-most edge.</li>
     * </ul>
     */
    public static Page target(Page current, Direction direction) {
        if (current == null || direction == null) {
            return null;
        }
        switch (current) {
            case ARTICLE:
                return direction == Direction.FORWARD ? Page.COMMENTS : Page.LIST;
            case COMMENTS:
                return direction == Direction.BACKWARD ? Page.ARTICLE : null;
            case LIST:
            default:
                return null;
        }
    }
}
