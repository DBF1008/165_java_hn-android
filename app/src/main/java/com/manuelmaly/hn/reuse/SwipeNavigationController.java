package com.manuelmaly.hn.reuse;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.manuelmaly.hn.reuse.SwipeNavigator.Direction;
import com.manuelmaly.hn.reuse.SwipeNavigator.Page;

/**
 * Reusable Android glue that turns horizontal fling gestures into swipe-navigation callbacks.
 * <p>
 * An activity creates one of these for the {@link Page} it represents and feeds every touch event
 * from {@code dispatchTouchEvent} into {@link #onTouchEvent(MotionEvent)}. The controller only ever
 * <em>observes</em> events - it never consumes them - so child views (ListView, WebView,
 * SwipeRefreshLayout) keep receiving and handling their own gestures exactly as before.
 * <p>
 * The decisions of whether a gesture is a swipe and where it leads are delegated to the
 * dependency-free {@link SwipeNavigator}, which is covered by unit tests.
 */
public class SwipeNavigationController {

    /** Minimum horizontal travel, in dp, for a fling to count as a navigation swipe. */
    private static final float MIN_DISTANCE_DP = 80f;

    /** Minimum horizontal speed, in dp/s, for a fling to count as a navigation swipe. */
    private static final float MIN_VELOCITY_DP = 200f;

    /** Notified when a swipe resolves to a real destination page. */
    public interface Callbacks {
        void onNavigate(Page target);
    }

    private final Page mCurrentPage;
    private final Callbacks mCallbacks;
    private final GestureDetector mGestureDetector;
    private final float mMinDistancePx;
    private final float mMinVelocityPx;

    public SwipeNavigationController(Context context, Page currentPage, Callbacks callbacks) {
        mCurrentPage = currentPage;
        mCallbacks = callbacks;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mMinDistancePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MIN_DISTANCE_DP, metrics);
        mMinVelocityPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MIN_VELOCITY_DP, metrics);

        mGestureDetector = new GestureDetector(context, new SwipeListener());
    }

    /**
     * Feeds a touch event to the gesture detector. The caller (an activity's
     * {@code dispatchTouchEvent}) must always pass the event on to {@code super} regardless of this
     * method's return value, so that child views are never starved of touch events.
     */
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    private class SwipeListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            // Required so the detector tracks the gesture stream and can emit onFling. The return
            // value only affects onTouchEvent()'s result, which the activity ignores - it never
            // consumes events - so this does not affect child views.
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) {
                return false;
            }

            Direction direction = SwipeNavigator.classifySwipe(
                    e2.getX() - e1.getX(), e2.getY() - e1.getY(), velocityX,
                    mMinDistancePx, mMinVelocityPx);
            if (direction == null) {
                return false;
            }

            Page targetPage = SwipeNavigator.target(mCurrentPage, direction);
            if (targetPage == null) {
                return false;
            }

            mCallbacks.onNavigate(targetPage);
            return true;
        }
    }
}
