package com.manuelmaly.hn.feed;

import com.manuelmaly.hn.model.HNFeed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns all multi-feed state and the decisions that go with it: which feed is current, the per-feed
 * {@link FeedSession}s, switching between feeds, applying load results with offline fallback, and
 * snapshotting for configuration changes.
 *
 * <p><b>Framework-free by design.</b> Every method takes/returns primitives or the pure model POJOs
 * ({@link HNFeed}); it never references {@code android.*} or the task/server layer (notably not
 * {@code ITaskFinishedHandler.TaskResultCode}). This is what allows the correctness-bearing logic to
 * be unit tested on a plain JVM. The Activity is a thin adapter that translates Android callbacks into
 * the boolean inputs this controller expects.
 */
public class FeedNavigationController {

    private final List<FeedType> mFeeds;
    private final Map<FeedType, FeedSession> mSessions;
    private FeedType mCurrent;

    public FeedNavigationController(List<FeedType> feeds, FeedType current) {
        mFeeds = new ArrayList<FeedType>(feeds);
        mSessions = new LinkedHashMap<FeedType, FeedSession>();
        if (current != null && mFeeds.contains(current)) {
            mCurrent = current;
        } else if (!mFeeds.isEmpty()) {
            mCurrent = mFeeds.get(0);
        } else {
            mCurrent = FeedType.getDefault();
        }
    }

    public List<FeedType> feeds() {
        return mFeeds;
    }

    public FeedType current() {
        return mCurrent;
    }

    /** Sets the current feed without touching any scroll state (used when restoring saved state). */
    public void setCurrent(FeedType type) {
        if (type != null && mFeeds.contains(type)) {
            mCurrent = type;
        }
    }

    /** Returns the session for a feed, creating an empty one on first access. */
    public FeedSession session(FeedType type) {
        FeedSession session = mSessions.get(type);
        if (session == null) {
            session = new FeedSession(type);
            mSessions.put(type, session);
        }
        return session;
    }

    public FeedSession currentSession() {
        return session(mCurrent);
    }

    /**
     * Switches to {@code type}, first stashing the outgoing feed's scroll position so it can be
     * restored when the user returns to it.
     */
    public void switchTo(FeedType type, Object outgoingScrollState) {
        currentSession().setScrollState(outgoingScrollState);
        if (type != null && mFeeds.contains(type)) {
            mCurrent = type;
        }
    }

    /**
     * Applies the outcome of a (full) feed load to a feed's session, with offline-cache fallback.
     *
     * <p>Decision table (cache is only accepted when it belongs to {@code currentUser}):
     * <pre>
     * success | result   | session has posts | gated cache | action
     * --------+----------+-------------------+-------------+-----------------------------------------
     * true    | nonempty | any               | any         | use loaded; fromCache=false; everLoaded=true
     * true    | empty    | yes               | any         | keep session
     * true    | empty    | no                | nonempty    | show cached; fromCache=true
     * true    | empty    | no                | none        | empty
     * false   | -        | yes               | any         | keep session
     * false   | -        | no                | nonempty    | show cached; fromCache=true
     * false   | -        | no                | none        | empty
     * </pre>
     *
     * A cancelled load should be passed as {@code success=false} so it falls into the "keep" rows.
     * {@code everLoaded} is set only on a live non-empty success.
     *
     * @param resultNonEmpty whether {@code loaded} has at least one post (computed by the caller)
     * @param cachedOrNull   the on-disk cached feed for this type, or null if none/unreadable
     */
    public void onLoaded(FeedType type, boolean success, boolean resultNonEmpty,
            String currentUser, HNFeed loaded, HNFeed cachedOrNull) {
        FeedSession session = session(type);
        session.setLoading(false);

        if (success && resultNonEmpty) {
            session.setFeed(loaded);
            session.setFromCache(false);
            session.setEverLoaded(true);
            return;
        }

        // Either the load failed, or it succeeded but returned nothing.
        // Never blank a feed that already has posts.
        if (!session.isEmpty()) {
            return;
        }

        HNFeed cache = acceptableCache(cachedOrNull, currentUser);
        if (cache != null) {
            session.setFeed(cache);
            session.setFromCache(true);
            // everLoaded intentionally stays false: cached data is not a live load.
            return;
        }

        session.setFeed(new HNFeed());
        session.setFromCache(false);
    }

    /** A cached feed is usable only if it has posts and was acquired for the current user. */
    private static HNFeed acceptableCache(HNFeed cached, String currentUser) {
        if (cached == null || cached.getPosts() == null || cached.getPosts().isEmpty()) {
            return null;
        }
        String acquiredFor = cached.getUserAcquiredFor();
        boolean sameUser = (acquiredFor == null) ? (currentUser == null) : acquiredFor.equals(currentUser);
        return sameUser ? cached : null;
    }

    // ---- snapshot for configuration change / process death ----

    /** Captures current selection, per-feed scroll positions and loaded feeds. */
    public FeedNavSnapshot save() {
        FeedNavSnapshot snapshot = new FeedNavSnapshot();
        snapshot.currentId = mCurrent.getId();
        for (Map.Entry<FeedType, FeedSession> entry : mSessions.entrySet()) {
            String id = entry.getKey().getId();
            FeedSession session = entry.getValue();
            if (session.getScrollState() != null) {
                snapshot.scrollByFeed.put(id, session.getScrollState());
            }
            if (!session.isEmpty()) {
                snapshot.feedByFeed.put(id, session.getFeed());
            }
        }
        return snapshot;
    }

    /** Restores selection, scroll positions and feeds from a snapshot. */
    public void restore(FeedNavSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        mCurrent = FeedType.fromId(snapshot.currentId, mCurrent);
        for (Map.Entry<String, Object> entry : snapshot.scrollByFeed.entrySet()) {
            FeedType type = FeedType.fromId(entry.getKey(), null);
            if (type != null) {
                session(type).setScrollState(entry.getValue());
            }
        }
        for (Map.Entry<String, HNFeed> entry : snapshot.feedByFeed.entrySet()) {
            FeedType type = FeedType.fromId(entry.getKey(), null);
            if (type != null) {
                session(type).setFeed(entry.getValue());
            }
        }
    }
}
