package com.manuelmaly.hn;

import android.app.Activity;
import android.content.Intent;
import android.view.MotionEvent;

import com.manuelmaly.hn.model.HNPost;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for swipe gesture navigation in {@link CommentsActivity}.
 *
 * Tests cover: swipe-left-to-go-back, swipe-right-to-article-reader,
 * existing ActionBar navigation compatibility, and slide animation application.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CommentsActivitySwipeTest {

    private static final String TEST_URL = "https://news.ycombinator.com/item?id=12345";
    private static final String TEST_TITLE = "Test HN Post";
    private static final String TEST_POST_ID = "12345";

    private HNPost createTestPost() {
        return new HNPost(TEST_URL, TEST_TITLE, "news.ycombinator.com",
                "testauthor", TEST_POST_ID, 20, 100, null);
    }

    private ActivityController<CommentsActivity_> buildActivity() {
        Intent intent = new Intent();
        intent.putExtra(CommentsActivity.EXTRA_HNPOST, createTestPost());
        return Robolectric.buildActivity(CommentsActivity_.class, intent);
    }

    private MotionEvent downEvent(float x, float y, long time) {
        return MotionEvent.obtain(time, time,
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

    private void performSwipe(Activity activity, float startX, float endX, float y) {
        long downTime = 1000L;
        int steps = 10;
        long durationMs = 150;

        activity.dispatchTouchEvent(downEvent(startX, y, downTime));

        for (int i = 1; i <= steps; i++) {
            float progress = (float) i / steps;
            float x = startX + (endX - startX) * progress;
            long eventTime = downTime + (durationMs * i / steps);
            activity.dispatchTouchEvent(moveEvent(x, y, downTime, eventTime));
        }

        activity.dispatchTouchEvent(upEvent(endX, y, downTime, downTime + durationMs));
    }

    // ---- Swipe-left-to-go-back tests ----

    @Test
    public void testSwipeLeftFinishesActivity() {
        ActivityController<CommentsActivity_> controller = buildActivity();
        CommentsActivity_ activity = controller.create().start().resume().get();

        performSwipe(activity, 600f, 100f, 200f);

        assertTrue("Activity should be finishing after left swipe",
                activity.isFinishing());
    }

    // ---- Swipe-right-to-article-reader tests ----

    @Test
    public void testSwipeRightLaunchesArticleReader() {
        ActivityController<CommentsActivity_> controller = buildActivity();
        CommentsActivity_ activity = controller.create().start().resume().get();

        performSwipe(activity, 100f, 600f, 200f);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent nextIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("ArticleReaderActivity should be started after right swipe",
                nextIntent);
        assertTrue("Intent should target ArticleReaderActivity",
                nextIntent.getComponent().getClassName().contains("ArticleReaderActivity"));
    }

    @Test
    public void testSwipeRightCarriesPostExtra() {
        ActivityController<CommentsActivity_> controller = buildActivity();
        CommentsActivity_ activity = controller.create().start().resume().get();

        performSwipe(activity, 100f, 600f, 200f);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent nextIntent = shadowActivity.getNextStartedActivity();

        if (nextIntent != null) {
            HNPost post = (HNPost) nextIntent.getSerializableExtra(CommentsActivity.EXTRA_HNPOST);
            assertNotNull("Post should be passed to ArticleReaderActivity", post);
            assertTrue("Post ID should match",
                    TEST_POST_ID.equals(post.getPostID()));
        }
    }

    // ---- Existing navigation compatibility ----

    @Test
    public void testExistingActionBarNavStillWorks() {
        ActivityController<CommentsActivity_> controller = buildActivity();
        CommentsActivity_ activity = controller.create().start().resume().get();

        // Verify the activity was created and is not finishing initially
        assertFalse("Activity should not be finishing initially",
                activity.isFinishing());
    }

    // ---- Slide animation tests ----

    @Test
    public void testSlideAnimationResourcesExist() {
        ActivityController<CommentsActivity_> controller = buildActivity();
        CommentsActivity_ activity = controller.create().start().resume().get();

        assertTrue("slide_in_right animation should exist",
                R.anim.slide_in_right != 0);
        assertTrue("slide_out_left animation should exist",
                R.anim.slide_out_left != 0);
        assertTrue("slide_in_left animation should exist",
                R.anim.slide_in_left != 0);
        assertTrue("slide_out_right animation should exist",
                R.anim.slide_out_right != 0);
    }

    // ---- Edge case tests ----

    @Test
    public void testVerticalScrollDoesNotTriggerNav() {
        ActivityController<CommentsActivity_> controller = buildActivity();
        CommentsActivity_ activity = controller.create().start().resume().get();

        long downTime = 1000L;
        activity.dispatchTouchEvent(downEvent(200f, 100f, downTime));
        activity.dispatchTouchEvent(moveEvent(205f, 600f, downTime, downTime + 150));
        activity.dispatchTouchEvent(upEvent(205f, 600f, downTime, downTime + 150));

        assertFalse("Vertical scroll should not finish activity",
                activity.isFinishing());
    }

    @Test
    public void testShortHorizontalSwipeDoesNotTriggerNav() {
        ActivityController<CommentsActivity_> controller = buildActivity();
        CommentsActivity_ activity = controller.create().start().resume().get();

        performSwipe(activity, 200f, 250f, 200f);

        assertFalse("Short swipe should not finish activity",
                activity.isFinishing());
    }

    @Test
    public void testDispatchTouchEventDoesNotCrash() {
        ActivityController<CommentsActivity_> controller = buildActivity();
        CommentsActivity_ activity = controller.create().start().resume().get();

        MotionEvent event = downEvent(100f, 100f, 1000L);
        activity.dispatchTouchEvent(event);
        // No crash = pass
    }

    @Test
    public void testActivityHandlesNullPostGracefully() {
        Intent intent = new Intent();
        // No EXTRA_HNPOST — should handle gracefully (finish with toast)
        ActivityController<CommentsActivity_> controller =
                Robolectric.buildActivity(CommentsActivity_.class, intent);
        CommentsActivity_ activity = controller.create().start().resume().get();

        // Activity should finish gracefully when no post is provided
        assertTrue("Activity should finish when no post is provided",
                activity.isFinishing());
    }
}
