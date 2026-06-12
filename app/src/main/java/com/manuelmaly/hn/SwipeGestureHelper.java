package com.manuelmaly.hn;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.core.view.GestureDetectorCompat;

/**
 * Reusable horizontal swipe gesture detector.
 * <p>
 * Attach via {@code dispatchTouchEvent} override in an Activity. Detects horizontal fling
 * gestures while ignoring vertical scrolling. Reports swipe direction via callback.
 * <p>
 * Thresholds are density-independent (dp-based) for consistent behavior across devices.
 */
public class SwipeGestureHelper {

    /**
     * Horizontal swipe direction.
     */
    public enum SwipeDirection {
        LEFT, RIGHT
    }

    /**
     * Callback interface for swipe events.
     */
    public interface OnSwipeListener {
        void onSwipe(SwipeDirection direction);
    }

    private static final int DEFAULT_MIN_DISTANCE_DP = 150;
    private static final int DEFAULT_MIN_VELOCITY_DP = 200;

    private final GestureDetectorCompat mDetector;
    private OnSwipeListener mListener;
    private boolean mEnabled = true;

    public SwipeGestureHelper(Context context, OnSwipeListener listener) {
        mListener = listener;
        mDetector = new GestureDetectorCompat(context, new SwipeListener(context));
    }

    /**
     * Call from {@code Activity.dispatchTouchEvent(MotionEvent)}.
     *
     * @param event the motion event
     * @return true if the gesture detector consumed the event
     */
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEnabled || mListener == null) {
            return false;
        }
        return mDetector.onTouchEvent(event);
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        mListener = listener;
    }

    private class SwipeListener extends GestureDetector.SimpleOnGestureListener {
        private final int mMinDistance;
        private final int mMinVelocity;

        SwipeListener(Context context) {
            float density = context.getResources().getDisplayMetrics().density;
            mMinDistance = (int) (DEFAULT_MIN_DISTANCE_DP * density);
            mMinVelocity = (int) (DEFAULT_MIN_VELOCITY_DP * density);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {
            if (e1 == null || e2 == null) {
                return false;
            }

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            // Only trigger if horizontal movement dominates vertical
            if (Math.abs(diffX) > Math.abs(diffY)
                    && Math.abs(diffX) > mMinDistance
                    && Math.abs(velocityX) > mMinVelocity) {

                if (diffX > 0) {
                    mListener.onSwipe(SwipeDirection.RIGHT);
                } else {
                    mListener.onSwipe(SwipeDirection.LEFT);
                }
                return true;
            }
            return false;
        }
    }
}
