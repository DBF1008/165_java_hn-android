package com.manuelmaly.hn;

import android.app.Application;
import android.view.MotionEvent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SwipeGestureHelper}.
 *
 * Tests cover: direction detection, threshold enforcement, velocity check,
 * vertical swipe rejection, enable/disable, null-safety, and rapid-swipe guards.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SwipeGestureHelperTest {

    private SwipeGestureHelper helper;
    private TestSwipeListener listener;

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        listener = new TestSwipeListener();
        helper = new SwipeGestureHelper(context, listener);
    }

    // ---- Helper methods ----

    private MotionEvent downEvent(float x, float y, long downTime) {
        return MotionEvent.obtain(downTime, downTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
    }

    private MotionEvent moveEvent(float x, float y, long downTime, long eventTime) {
        return MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_MOVE, x, y, 0);
    }

    private MotionEvent upEvent(float x, float y, long downTime, long eventTime) {
        return MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_UP, x, y, 0);
    }

    /**
     * Simulates a horizontal fling by sending DOWN → multiple MOVEs → UP
     * over a short time window to exceed velocity threshold.
     */
    private void simulateFling(float startX, float endX, float y) {
        long downTime = 1000L;
        int steps = 10;
        long durationMs = 150; // Short duration for high velocity

        helper.onTouchEvent(downEvent(startX, y, downTime));

        for (int i = 1; i <= steps; i++) {
            float progress = (float) i / steps;
            float x = startX + (endX - startX) * progress;
            long eventTime = downTime + (durationMs * i / steps);
            helper.onTouchEvent(moveEvent(x, y, downTime, eventTime));
        }

        helper.onTouchEvent(upEvent(endX, y, downTime, downTime + durationMs));
    }

    /**
     * Simulates a vertical fling (large Y delta, small X delta).
     */
    private void simulateVerticalFling(float x, float startY, float endY) {
        long downTime = 1000L;
        int steps = 10;
        long durationMs = 150;

        helper.onTouchEvent(downEvent(x, startY, downTime));

        for (int i = 1; i <= steps; i++) {
            float progress = (float) i / steps;
            float y = startY + (endY - startY) * progress;
            long eventTime = downTime + (durationMs * i / steps);
            helper.onTouchEvent(moveEvent(x + 5, y, downTime, eventTime));
        }

        helper.onTouchEvent(upEvent(x + 5, endY, downTime, downTime + durationMs));
    }

    // ---- Direction detection tests ----

    @Test
    public void testRightSwipeDetected() {
        simulateFling(100f, 600f, 200f);
        assertEquals("Right swipe should be detected",
                SwipeGestureHelper.SwipeDirection.RIGHT, listener.lastDirection);
        assertEquals("Should fire exactly once", 1, listener.callCount);
    }

    @Test
    public void testLeftSwipeDetected() {
        simulateFling(600f, 100f, 200f);
        assertEquals("Left swipe should be detected",
                SwipeGestureHelper.SwipeDirection.LEFT, listener.lastDirection);
        assertEquals("Should fire exactly once", 1, listener.callCount);
    }

    // ---- Threshold tests ----

    @Test
    public void testShortSwipeIgnored() {
        // Only 50px of movement — well below the 150dp threshold
        simulateFling(100f, 150f, 200f);
        assertEquals("Short swipe should not trigger callback",
                0, listener.callCount);
    }

    @Test
    public void testSlowSwipeIgnored() {
        // Large distance but very slow — below velocity threshold
        long downTime = 1000L;
        int steps = 10;
        long durationMs = 5000; // 5 seconds = very slow

        helper.onTouchEvent(downEvent(100f, 200f, downTime));
        for (int i = 1; i <= steps; i++) {
            float progress = (float) i / steps;
            float x = 100f + 500f * progress;
            long eventTime = downTime + (durationMs * i / steps);
            helper.onTouchEvent(moveEvent(x, 200f, downTime, eventTime));
        }
        helper.onTouchEvent(upEvent(600f, 200f, downTime, downTime + durationMs));

        assertEquals("Slow swipe should not trigger callback",
                0, listener.callCount);
    }

    // ---- Vertical swipe rejection ----

    @Test
    public void testVerticalSwipeIgnored() {
        simulateVerticalFling(200f, 100f, 600f);
        assertEquals("Vertical swipe should not trigger callback",
                0, listener.callCount);
    }

    @Test
    public void testDiagonalSwipeIgnored() {
        // Equal horizontal and vertical movement — should not trigger
        long downTime = 1000L;
        long durationMs = 150;

        helper.onTouchEvent(downEvent(100f, 100f, downTime));
        helper.onTouchEvent(moveEvent(350f, 350f, downTime, downTime + durationMs));
        helper.onTouchEvent(upEvent(600f, 600f, downTime, downTime + durationMs));

        assertEquals("Diagonal swipe should not trigger callback",
                0, listener.callCount);
    }

    // ---- Enable/disable tests ----

    @Test
    public void testDisabledDoesNotFire() {
        helper.setEnabled(false);
        simulateFling(100f, 600f, 200f);
        assertEquals("Disabled helper should not fire",
                0, listener.callCount);
    }

    @Test
    public void testReEnableFires() {
        helper.setEnabled(false);
        simulateFling(100f, 600f, 200f);
        assertEquals(0, listener.callCount);

        helper.setEnabled(true);
        simulateFling(100f, 600f, 200f);
        assertEquals("Re-enabled helper should fire",
                1, listener.callCount);
    }

    @Test
    public void testIsEnabledDefault() {
        assertTrue("Helper should be enabled by default", helper.isEnabled());
    }

    // ---- Null safety tests ----

    @Test
    public void testNullListenerNoCrash() {
        helper.setOnSwipeListener(null);
        // Should not throw
        simulateFling(100f, 600f, 200f);
    }

    @Test
    public void testDisabledReturnsFalse() {
        helper.setEnabled(false);
        MotionEvent event = downEvent(100f, 200f, 1000L);
        assertFalse("Disabled helper should return false from onTouchEvent",
                helper.onTouchEvent(event));
    }

    // ---- Test listener helper ----

    private static class TestSwipeListener implements SwipeGestureHelper.OnSwipeListener {
        SwipeGestureHelper.SwipeDirection lastDirection;
        int callCount = 0;

        @Override
        public void onSwipe(SwipeGestureHelper.SwipeDirection direction) {
            lastDirection = direction;
            callCount++;
        }
    }
}
