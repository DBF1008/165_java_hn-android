package com.manuelmaly.hn;

import com.manuelmaly.hn.feed.FeedNavigationController;
import com.manuelmaly.hn.feed.FeedSession;
import com.manuelmaly.hn.feed.FeedType;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.parser.BaseHTMLParser;
import com.manuelmaly.hn.server.HNCredentials;
import com.manuelmaly.hn.task.HNFeedTask;
import com.manuelmaly.hn.task.HNFeedTaskLoadMore;
import com.manuelmaly.hn.task.HNVoteTask;
import com.manuelmaly.hn.task.ITaskFinishedHandler;
import com.manuelmaly.hn.util.FileUtil;
import com.manuelmaly.hn.util.FontHelper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.DataSetObserver;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.core.view.MenuItemCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EActivity(R.layout.main)
public class MainActivity extends BaseListActivity implements
        ITaskFinishedHandler<HNFeed> {

    @ViewById(R.id.main_list)
    ListView mPostsList;

    @ViewById(R.id.main_root)
    LinearLayout mRootView;

    @ViewById(R.id.main_feed_tabs)
    LinearLayout mFeedTabs;

    @ViewById(R.id.main_swiperefreshlayout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @SystemService
    LayoutInflater mInflater;

    TextView mEmptyListPlaceholder;
    /** Owns all per-feed state (current feed, per-feed posts/scroll, offline fallback). */
    FeedNavigationController mNav;
    Map<FeedType, TextView> mTabViews;
    PostsAdapter mPostsListAdapter;
    Set<HNPost> mUpvotedPosts;
    Set<Integer> mAlreadyRead;

    String mCurrentFontSize = null;
    int mFontSizeTitle;
    int mFontSizeDetails;
    int mTitleColor;
    int mTitleReadColor;

    private static final int TASKCODE_LOAD_FEED = 10;
    private static final int TASKCODE_LOAD_MORE_POSTS = 20;
    private static final int TASKCODE_VOTE = 100;

    private static final String STATE_CURRENT_FEED = "currentFeed";
    private static final String STATE_SCROLL_PREFIX = "scroll_";
    private static final String ALREADY_READ_ARTICLES_KEY = "HN_ALREADY_READ";

    /**
     * When true, the current feed's saved scroll position should be re-applied once its list has
     * data. Set on a feed switch and on state restore; consumed by {@link #applyPendingScrollIfNeeded()}.
     * It is intentionally NOT set on a plain refresh, so refreshing doesn't jump the user around.
     */
    private boolean mPendingScrollRestore = false;

    boolean mShouldShowRefreshing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure that we show the overflow menu icon
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class
                    .getDeclaredField("sHasPermanentMenuKey");

            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            // presumably, not relevant
        }

        TextView tv = (TextView) getSupportActionBar().getCustomView()
                .findViewById(R.id.actionbar_title);
        tv.setTypeface(FontHelper.getComfortaa(this, true));
    }

    @AfterViews
    public void init() {
        FeedType initial = FeedType.fromId(Settings.getSelectedFeedId(this), FeedType.getDefault());
        mNav = new FeedNavigationController(FeedType.ordered(), initial);

        mPostsListAdapter = new PostsAdapter();
        mUpvotedPosts = new HashSet<HNPost>();

        mEmptyListPlaceholder = getEmptyTextView(mRootView);
        mPostsList.setEmptyView(mEmptyListPlaceholder);
        mPostsList.setAdapter(mPostsListAdapter);

        mEmptyListPlaceholder.setTypeface(FontHelper.getComfortaa(this, true));

        mTitleColor = getResources().getColor(R.color.dark_gray_post_title);
        mTitleReadColor = getResources().getColor(R.color.gray_post_title_read);

        buildFeedTabs();

        toggleSwipeRefreshLayout();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startFeedLoading();
            }
        });

        loadAlreadyReadCache();
        // The actual feed load happens in onResume(), so it runs against the feed selected by any
        // restored instance state rather than always the settings default.
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload everything if a new user logged in (upvote URLs are user-specific).
        HNFeed currentFeed = mNav.currentSession().getFeed();
        boolean registeredUserChanged = currentFeed.getUserAcquiredFor() != null
                && !currentFeed.getUserAcquiredFor().equals(Settings.getUserName(this));

        if (HNCredentials.isInvalidated() || registeredUserChanged) {
            for (FeedType type : mNav.feeds()) {
                FeedSession session = mNav.session(type);
                session.setFeed(new HNFeed());
                session.setFromCache(false);
                session.setEverLoaded(false);
            }
            mPostsListAdapter.notifyDataSetChanged();
        }

        // refresh if font size changed
        if (refreshFontSizes()) {
            mPostsListAdapter.notifyDataSetChanged();
        }

        updateTabHighlight();
        ensureCurrentFeedLoaded();
        applyPendingScrollIfNeeded();

        // User may have toggled pull-down refresh, so toggle the SwipeRefreshLayout.
        toggleSwipeRefreshLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_refresh);

        if (!mShouldShowRefreshing) {
            MenuItemCompat.setActionView(item, null);
        } else {
            View v = mInflater.inflate(R.layout.refresh_icon, null);
            MenuItemCompat.setActionView(item, v);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        case R.id.menu_about:
            startActivity(new Intent(MainActivity.this, AboutActivity_.class));
            return true;
        case R.id.menu_refresh:
            startFeedLoading();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggleSwipeRefreshLayout() {
        mSwipeRefreshLayout.setEnabled(Settings.isPullDownRefresh(MainActivity.this));
    }

    // ------------------------------------------------------------------
    // Feed category tab strip
    // ------------------------------------------------------------------

    private void buildFeedTabs() {
        mTabViews = new EnumMap<FeedType, TextView>(FeedType.class);
        mFeedTabs.removeAllViews();

        int padH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                getResources().getDisplayMetrics());
        int padV = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
                getResources().getDisplayMetrics());

        for (final FeedType type : mNav.feeds()) {
            TextView tab = new TextView(this);
            tab.setText(type.getTitle());
            tab.setTypeface(FontHelper.getComfortaa(this, true));
            tab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            tab.setPadding(padH, padV, padH, padV);
            tab.setGravity(Gravity.CENTER);
            tab.setClickable(true);
            tab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectFeed(type);
                }
            });
            mFeedTabs.addView(tab);
            mTabViews.put(type, tab);
        }

        updateTabHighlight();
    }

    private void updateTabHighlight() {
        if (mTabViews == null) {
            return;
        }
        for (Map.Entry<FeedType, TextView> entry : mTabViews.entrySet()) {
            boolean selected = entry.getKey() == mNav.current();
            TextView tab = entry.getValue();
            tab.setTextColor(selected ? mTitleColor : mTitleReadColor);
            if (selected) {
                tab.setPaintFlags(tab.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            } else {
                tab.setPaintFlags(tab.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
            }
        }
    }

    /** Switches the visible feed, preserving the outgoing feed's scroll and lazily loading the new one. */
    private void selectFeed(FeedType type) {
        if (type == mNav.current()) {
            return;
        }

        // Stash the scroll position of the feed we're leaving, then switch.
        Parcelable outgoingScroll = mPostsList.onSaveInstanceState();
        mNav.switchTo(type, outgoingScroll);
        Settings.setSelectedFeedId(this, type.getId());

        updateTabHighlight();
        mPendingScrollRestore = true;
        mPostsListAdapter.notifyDataSetChanged();
        applyPendingScrollIfNeeded();

        FeedSession session = mNav.currentSession();
        if (session.isEmpty()) {
            if (HNFeedTask.isRunning(this, type)) {
                setShowRefreshing(true);
            } else {
                loadCachedFeed(type);
                startFeedLoading();
            }
        } else {
            setShowRefreshing(session.isLoading());
        }
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    private void ensureCurrentFeedLoaded() {
        FeedSession session = mNav.currentSession();
        if (session.isEmpty() && !HNFeedTask.isRunning(this, mNav.current())) {
            loadCachedFeed(mNav.current());
            startFeedLoading();
        }
    }

    private void startFeedLoading() {
        FeedType type = mNav.current();
        mNav.currentSession().setLoading(true);
        setShowRefreshing(true);
        HNFeedTask.startOrReattach(this, this, type, TASKCODE_LOAD_FEED);
    }

    /** Kicks an async read of this feed's offline cache for a fast first paint. */
    private void loadCachedFeed(FeedType type) {
        new GetLastHNFeedTask(type).execute((Void) null);
    }

    private HNFeed currentFeed() {
        return mNav.currentSession().getFeed();
    }

    /** True if a cached feed may be shown for the current user (mirrors the live-load gating). */
    private boolean isCacheAcceptable(HNFeed cached) {
        return cached != null
                && cached.getPosts() != null
                && !cached.getPosts().isEmpty()
                && cached.getUserAcquiredFor() != null
                && cached.getUserAcquiredFor().equals(Settings.getUserName(this));
    }

    @Override
    public void onTaskFinished(int taskCode, TaskResultCode code, HNFeed result, Object tag) {
        FeedType type = (tag instanceof FeedType) ? (FeedType) tag : mNav.current();
        boolean isCurrent = (type == mNav.current());

        if (taskCode == TASKCODE_LOAD_FEED) {
            boolean success = code.equals(TaskResultCode.Success) && result != null;
            boolean resultNonEmpty = result != null && result.getPosts() != null
                    && !result.getPosts().isEmpty();

            // Cache is supplied via the async loadCachedFeed() paint, so pass null here; the
            // controller's "keep what we have" rule preserves any already-painted cached posts.
            mNav.onLoaded(type, success, resultNonEmpty, Settings.getUserName(this), result, null);

            if (isCurrent && mPostsListAdapter != null) {
                mPostsListAdapter.notifyDataSetChanged();
                applyPendingScrollIfNeeded();
            }

            if (isCurrent && !success && !code.equals(TaskResultCode.CancelledByUser)
                    && mNav.session(type).isEmpty()) {
                Toast.makeText(this, getString(R.string.error_unable_to_retrieve_feed),
                        Toast.LENGTH_SHORT).show();
            }
        } else if (taskCode == TASKCODE_LOAD_MORE_POSTS) {
            FeedSession session = mNav.session(type);
            if (!code.equals(TaskResultCode.Success) || result == null || result.getPosts() == null
                    || result.getPosts().size() == 0) {
                if (isCurrent) {
                    Toast.makeText(this, getString(R.string.error_unable_to_load_more),
                            Toast.LENGTH_SHORT).show();
                }
                session.getFeed().setLoadedMore(true); // reached the end.
            }
            session.getFeed().appendLoadMoreFeed(result);
            if (isCurrent) {
                mPostsListAdapter.notifyDataSetChanged();
            }
        }

        if (isCurrent) {
            mNav.currentSession().setLoading(false);
            setShowRefreshing(false);
        }
    }

    /** Re-applies the current feed's saved scroll once its list has content (used after switch/restore). */
    private void applyPendingScrollIfNeeded() {
        if (!mPendingScrollRestore) {
            return;
        }
        FeedSession session = mNav.currentSession();
        if (session.isEmpty()) {
            return; // wait until data lands, then this is called again
        }
        mPendingScrollRestore = false;
        Object scroll = session.getScrollState();
        if (scroll instanceof Parcelable) {
            final Parcelable parcelable = (Parcelable) scroll;
            mPostsList.post(new Runnable() {
                @Override
                public void run() {
                    mPostsList.onRestoreInstanceState(parcelable);
                }
            });
        }
    }

    @Background
    void loadAlreadyReadCache() {
        if (mAlreadyRead == null) {
            mAlreadyRead = new HashSet<Integer>();
        }

        SharedPreferences sharedPref = getSharedPreferences(
                ALREADY_READ_ARTICLES_KEY, Context.MODE_PRIVATE);
        Editor editor = sharedPref.edit();
        Map<String, ?> read = sharedPref.getAll();
        Long now = new Date().getTime();

        for (Map.Entry<String, ?> entry : read.entrySet()) {
            Long readAt = (Long) entry.getValue();
            Long diff = (now - readAt) / (24 * 60 * 60 * 1000);
            if (diff >= 2) {
                editor.remove(entry.getKey());
            } else {
                mAlreadyRead.add(entry.getKey().hashCode());
            }
        }
        editor.commit();
    }

    @Background
    void markAsRead(HNPost post) {
        Long now = new Date().getTime();
        String title = post.getTitle();
        Editor editor = getSharedPreferences(ALREADY_READ_ARTICLES_KEY,
                Context.MODE_PRIVATE).edit();
        editor.putLong(title, now);
        editor.commit();

        mAlreadyRead.add(title.hashCode());
    }

    /**
     * Reads a feed's offline cache off the UI thread; paints it only if the feed hasn't already been
     * populated by a live load and the cache belongs to the current user.
     */
    class GetLastHNFeedTask extends FileUtil.GetLastHNFeedTask {
        private final FeedType mType;

        GetLastHNFeedTask(FeedType type) {
            super(type);
            mType = type;
        }

        @Override
        protected void onPostExecute(HNFeed result) {
            if (result == null) {
                return;
            }
            FeedSession session = mNav.session(mType);
            if (session.isEmpty() && isCacheAcceptable(result)) {
                session.setFeed(result);
                session.setFromCache(true);
                if (mType == mNav.current()) {
                    mPostsListAdapter.notifyDataSetChanged();
                    applyPendingScrollIfNeeded();
                }
            }
        }
    }

    private boolean refreshFontSizes() {
        final String fontSize = Settings.getFontSize(this);
        if ((mCurrentFontSize == null) || (!mCurrentFontSize.equals(fontSize))) {
            mCurrentFontSize = fontSize;
            if (fontSize.equals(getString(R.string.pref_fontsize_small))) {
                mFontSizeTitle = 15;
                mFontSizeDetails = 11;
            } else
                if (fontSize.equals(getString(R.string.pref_fontsize_normal))) {
                    mFontSizeTitle = 18;
                    mFontSizeDetails = 12;
                } else {
                    mFontSizeTitle = 22;
                    mFontSizeDetails = 15;
                }
            return true;
        } else {
            return false;
        }
    }

    private void vote(String voteURL, HNPost post) {
        HNVoteTask.start(voteURL, MainActivity.this,
                new VoteTaskFinishedHandler(), TASKCODE_VOTE, post);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        String currentId = state.getString(STATE_CURRENT_FEED);
        if (currentId != null) {
            mNav.setCurrent(FeedType.fromId(currentId, mNav.current()));
        }
        for (FeedType type : mNav.feeds()) {
            Parcelable scroll = state.getParcelable(STATE_SCROLL_PREFIX + type.getId());
            if (scroll != null) {
                mNav.session(type).setScrollState(scroll);
            }
        }
        mPendingScrollRestore = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        // Capture the currently visible scroll into the current session first.
        mNav.currentSession().setScrollState(mPostsList.onSaveInstanceState());

        state.putString(STATE_CURRENT_FEED, mNav.current().getId());
        for (FeedType type : mNav.feeds()) {
            Object scroll = mNav.session(type).getScrollState();
            if (scroll instanceof Parcelable) {
                state.putParcelable(STATE_SCROLL_PREFIX + type.getId(), (Parcelable) scroll);
            }
        }
        // Feed post data is intentionally NOT serialized into the Bundle (it can be large); it is
        // restored from each feed's file cache and a fresh network load on recreation.
    }

    class VoteTaskFinishedHandler implements ITaskFinishedHandler<Boolean> {
        @Override
        public void onTaskFinished(
                int taskCode,
                com.manuelmaly.hn.task.ITaskFinishedHandler.TaskResultCode code,
                Boolean result, Object tag) {
            if (taskCode == TASKCODE_VOTE) {
                if (result != null && result.booleanValue()) {
                    Toast.makeText(MainActivity.this, R.string.vote_success,
                            Toast.LENGTH_SHORT).show();
                    HNPost post = (HNPost) tag;
                    if (post != null) {
                        mUpvotedPosts.add(post);
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.vote_error,
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    class PostsAdapter extends BaseAdapter {

        private static final int VIEWTYPE_POST = 0;
        private static final int VIEWTYPE_LOADMORE = 1;
        private static final String HACKERNEWS_URLDOMAIN = "news.ycombinator.com";

        @Override
        public int getCount() {
            int posts = currentFeed().getPosts().size();
            if (posts == 0) {
                return 0;
            } else {
                return posts + (currentFeed().isLoadedMore() ? 0 : 1);
            }
        }

        @Override
        public HNPost getItem(int position) {
            if (getItemViewType(position) == VIEWTYPE_POST) {
                return currentFeed().getPosts().get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            // Item ID not needed here:
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            if (position < currentFeed().getPosts().size()) {
                return VIEWTYPE_POST;
            } else {
                return VIEWTYPE_LOADMORE;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(final int position, View convertView,
                ViewGroup parent) {
            switch (getItemViewType(position)) {
            case VIEWTYPE_POST:
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.main_list_item,
                            null);
                    PostViewHolder holder = new PostViewHolder();
                    holder.titleView = (TextView) convertView
                            .findViewById(R.id.main_list_item_title);
                    holder.urlView = (TextView) convertView
                            .findViewById(R.id.main_list_item_url);
                    holder.textContainer = (LinearLayout) convertView
                            .findViewById(R.id.main_list_item_textcontainer);
                    holder.commentsButton = (Button) convertView
                            .findViewById(R.id.main_list_item_comments_button);
                    holder.commentsButton.setTypeface(FontHelper.getComfortaa(
                            MainActivity.this, false));
                    holder.pointsView = (TextView) convertView
                            .findViewById(R.id.main_list_item_points);
                    holder.pointsView.setTypeface(FontHelper.getComfortaa(
                            MainActivity.this, true));
                    convertView.setTag(holder);
                }

                final HNPost item = getItem(position);
                PostViewHolder holder = (PostViewHolder) convertView.getTag();
                holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                        mFontSizeTitle);
                holder.titleView.setText(item.getTitle());
                holder.titleView.setTextColor(isRead(item) ? mTitleReadColor
                        : mTitleColor);
                holder.urlView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                        mFontSizeDetails);
                holder.urlView.setText(item.getURLDomain());
                holder.pointsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                        mFontSizeDetails);
                if (item.getPoints() != BaseHTMLParser.UNDEFINED) {
                    holder.pointsView.setText(item.getPoints() + "");
                } else {
                    holder.pointsView.setText("-");
                }

                holder.commentsButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                        mFontSizeTitle);
                if (item.getCommentsCount() != BaseHTMLParser.UNDEFINED) {
                    holder.commentsButton.setVisibility(View.VISIBLE);
                    holder.commentsButton.setText(item.getCommentsCount() + "");
                } else {
                    holder.commentsButton.setVisibility(View.INVISIBLE);
                }
                holder.commentsButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startCommentActivity(position);
                    }
                });
                holder.textContainer.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        markAsRead(item);
                        if(getItem(position).getURLDomain().equals(HACKERNEWS_URLDOMAIN)){
                            startCommentActivity(position);
                        }
                        else  if (Settings.getHtmlViewer(MainActivity.this).equals(
                                getString(R.string.pref_htmlviewer_browser))) {
                            openURLInBrowser(
                                    getArticleViewURL(getItem(position)),
                                    MainActivity.this);
                        } else {
                            openPostInApp(getItem(position), null,
                                    MainActivity.this);
                        }
                    }
                });
                holder.textContainer
                        .setOnLongClickListener(new OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                final HNPost post = getItem(position);

                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                        MainActivity.this);
                                LongPressMenuListAdapter adapter = new LongPressMenuListAdapter(
                                        post);
                                builder.setAdapter(adapter, adapter).show();
                                return true;
                            }
                        });
                break;

            case VIEWTYPE_LOADMORE:
                // I don't use the preloaded convertView here because it's
                // only one cell
                convertView = mInflater.inflate(
                        R.layout.main_list_item_loadmore, null);
                final TextView textView = (TextView) convertView
                        .findViewById(R.id.main_list_item_loadmore_text);
                textView.setTypeface(FontHelper.getComfortaa(MainActivity.this,
                        true));
                final ImageView imageView = (ImageView) convertView
                        .findViewById(R.id.main_list_item_loadmore_loadingimage);
                if (HNFeedTaskLoadMore.isRunning(MainActivity.this,
                        mNav.current())) {
                    textView.setVisibility(View.INVISIBLE);
                    imageView.setVisibility(View.VISIBLE);
                    convertView.setClickable(false);
                }

                final View convertViewFinal = convertView;
                convertView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        textView.setVisibility(View.INVISIBLE);
                        imageView.setVisibility(View.VISIBLE);
                        convertViewFinal.setClickable(false);
                        HNFeedTaskLoadMore.start(MainActivity.this,
                                MainActivity.this, currentFeed(),
                                mNav.current(), TASKCODE_LOAD_MORE_POSTS);
                        setShowRefreshing(true);
                    }
                });
                break;
            default:
                break;
            }

            return convertView;
        }

        private boolean isRead(HNPost post) {
            return mAlreadyRead.contains(post.getTitle().hashCode());
        }

        private void startCommentActivity(int position){
            Intent i = new Intent(MainActivity.this,
                    CommentsActivity_.class);
            i.putExtra(CommentsActivity.EXTRA_HNPOST,
                    getItem(position));
            startActivity(i);
        }
    }

    private class LongPressMenuListAdapter implements ListAdapter,
            DialogInterface.OnClickListener {

        HNPost mPost;
        boolean mIsLoggedIn;
        boolean mUpVotingEnabled;
        ArrayList<CharSequence> mItems;

        public LongPressMenuListAdapter(HNPost post) {
            mPost = post;
            mIsLoggedIn = Settings.isUserLoggedIn(MainActivity.this);
            mUpVotingEnabled = !mIsLoggedIn
                    || (mPost.getUpvoteURL(Settings
                            .getUserName(MainActivity.this)) != null && !mUpvotedPosts
                            .contains(mPost));

            mItems = new ArrayList<CharSequence>();
            if (mUpVotingEnabled) {
                mItems.add(getString(R.string.upvote));
            } else {
                mItems.add(getString(R.string.already_upvoted));
            }
            mItems.addAll(Arrays.asList(
                    getString(R.string.pref_htmlprovider_original_url),
                    getString(R.string.pref_htmlprovider_viewtext),
                    getString(R.string.pref_htmlprovider_google),
                    getString(R.string.pref_htmlprovider_instapaper),
                    getString(R.string.external_browser),
                    getString(R.string.share_article_url)));
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public CharSequence getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) mInflater.inflate(
                    android.R.layout.simple_list_item_1, null);
            view.setText(getItem(position));
            if (!mUpVotingEnabled && position == 0) {
                view.setTextColor(getResources().getColor(
                        android.R.color.darker_gray));
            }
            return view;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (!mUpVotingEnabled && position == 4) {
                return false;
            }
            return true;
        }

        @Override
        public void onClick(DialogInterface dialog, int item) {
            switch (item) {
            case 0:
                if (!mIsLoggedIn) {
                    Toast.makeText(MainActivity.this, R.string.please_log_in,
                            Toast.LENGTH_LONG).show();
                } else
                    if (mUpVotingEnabled) {
                        vote(mPost.getUpvoteURL(Settings
                                .getUserName(MainActivity.this)), mPost);
                    }
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                openPostInApp(mPost, getItem(item).toString(),
                        MainActivity.this);
                markAsRead(mPost);
                break;
            case 5:
                openURLInBrowser(getArticleViewURL(mPost), MainActivity.this);
                markAsRead(mPost);
                break;
            case 6:
                shareUrl(mPost, MainActivity.this);
                break;
            default:
                break;
            }
        }

    }

    private String getArticleViewURL(HNPost post) {
        return ArticleReaderActivity.getArticleViewURL(post,
                Settings.getHtmlProvider(this), this);
    }

    public static void openURLInBrowser(String url, Activity a) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        a.startActivity(browserIntent);
    }

    public static void openPostInApp(HNPost post, String overrideHtmlProvider,
            Activity a) {
        Intent i = new Intent(a, ArticleReaderActivity_.class);
        i.putExtra(ArticleReaderActivity.EXTRA_HNPOST, post);
        if (overrideHtmlProvider != null) {
            i.putExtra(ArticleReaderActivity.EXTRA_HTMLPROVIDER_OVERRIDE,
                    overrideHtmlProvider);
        }
        a.startActivity(i);
    }

    public static void shareUrl(HNPost post, Activity a){
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType("text/plain");
      shareIntent.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
      shareIntent.putExtra(Intent.EXTRA_TEXT, post.getURL());
      a.startActivity(Intent.createChooser(shareIntent, a.getString(R.string.share_article_url)));
    }

    private void setShowRefreshing(boolean showRefreshing) {
        if (!Settings.isPullDownRefresh(MainActivity.this)) {
            mShouldShowRefreshing = showRefreshing;
            supportInvalidateOptionsMenu();
        }

        if (mSwipeRefreshLayout.isEnabled() && (!mSwipeRefreshLayout.isRefreshing() || !showRefreshing)) {
            mSwipeRefreshLayout.setRefreshing(showRefreshing);
        }
    }

    static class PostViewHolder {
        TextView titleView;
        TextView urlView;
        TextView pointsView;
        TextView commentsCountView;
        LinearLayout textContainer;
        Button commentsButton;
    }

}
