package com.manuelmaly.hn;

import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.server.HNCredentials;
import com.manuelmaly.hn.ui.PostsAdapter;
import com.manuelmaly.hn.util.FontHelper;
import com.manuelmaly.hn.viewmodel.HNViewModelFactory;
import com.manuelmaly.hn.viewmodel.MainViewModel;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.core.view.MenuItemCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Feed screen. Following the MVVM refactor this is a thin view: list rendering
 * lives in {@link PostsAdapter} and all data processing (loading, load-more,
 * voting, read-state, caching) lives in {@link MainViewModel}, observed here via
 * LiveData. The Activity only wires views, forwards user intents, and keeps the
 * inherently framework-bound pieces (the long-press menu, share/browser intents,
 * the refresh chrome and list-scroll restore).
 */
@EActivity(R.layout.main)
public class MainActivity extends BaseListActivity implements PostsAdapter.Host {

    @ViewById(R.id.main_list)
    ListView mPostsList;

    @ViewById(R.id.main_root)
    LinearLayout mRootView;

    @ViewById(R.id.main_swiperefreshlayout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @SystemService
    LayoutInflater mInflater;

    TextView mEmptyListPlaceholder;
    PostsAdapter mPostsListAdapter;
    MainViewModel mViewModel;

    String mCurrentFontSize = null;
    int mFontSizeTitle;
    int mFontSizeDetails;
    int mTitleColor;
    int mTitleReadColor;

    private static final String LIST_STATE = "listState";
    private static final String HACKERNEWS_URLDOMAIN = "news.ycombinator.com";
    private Parcelable mListState = null;

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
        HNViewModelFactory factory = new HNViewModelFactory(getApplicationContext());
        mViewModel = new ViewModelProvider(this, factory).get(MainViewModel.class);

        mPostsListAdapter = new PostsAdapter(this, this);

        mEmptyListPlaceholder = getEmptyTextView(mRootView);
        mPostsList.setEmptyView(mEmptyListPlaceholder);
        mPostsList.setAdapter(mPostsListAdapter);
        mEmptyListPlaceholder.setTypeface(FontHelper.getComfortaa(this, true));

        mTitleColor = getResources().getColor(R.color.dark_gray_post_title);
        mTitleReadColor = getResources().getColor(R.color.gray_post_title_read);
        refreshFontSizes();

        toggleSwipeRefreshLayout();
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mViewModel.loadFeed();
            }
        });

        observeViewModel();

        mViewModel.loadAlreadyRead();
        mViewModel.loadCached(Settings.getUserName(this));
        mViewModel.loadFeed();
    }

    private void observeViewModel() {
        mViewModel.getFeed().observe(this, new Observer<Resource<HNFeed>>() {
            @Override
            public void onChanged(Resource<HNFeed> resource) {
                mPostsListAdapter.notifyDataSetChanged();
            }
        });
        mViewModel.getRefreshing().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean refreshing) {
                setShowRefreshing(Boolean.TRUE.equals(refreshing));
            }
        });
        mViewModel.getLoadMoreInFlight().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean inFlight) {
                // Re-render so the load-more row reflects the new in-flight state.
                mPostsListAdapter.notifyDataSetChanged();
            }
        });
        mViewModel.getVoteEvent().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(MainActivity.this, R.string.vote_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.vote_error, Toast.LENGTH_LONG).show();
                }
            }
        });
        mViewModel.getErrorMessage().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer messageResId) {
                if (messageResId != null) {
                    Toast.makeText(MainActivity.this, messageResId, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        HNFeed feed = mViewModel.currentFeed();
        boolean registeredUserChanged = feed != null && feed.getUserAcquiredFor() != null
                && (!feed.getUserAcquiredFor().equals(Settings.getUserName(this)));

        // We want to reload the feed if a new user logged in
        if (HNCredentials.isInvalidated() || registeredUserChanged) {
            mViewModel.resetFeed();
            mViewModel.loadFeed();
        }

        // refresh if font size changed
        if (refreshFontSizes()) {
            mPostsListAdapter.notifyDataSetChanged();
        }

        // restore vertical scrolling position if applicable
        if (mListState != null) {
            mPostsList.onRestoreInstanceState(mListState);
        }
        mListState = null;

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
            mViewModel.loadFeed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggleSwipeRefreshLayout() {
        mSwipeRefreshLayout.setEnabled(Settings.isPullDownRefresh(MainActivity.this));
    }

    // --- PostsAdapter.Host -------------------------------------------------

    @Override
    public HNFeed getFeed() {
        return mViewModel.currentFeed();
    }

    @Override
    public boolean isRead(HNPost post) {
        return mViewModel.isRead(post);
    }

    @Override
    public boolean isLoadMoreInFlight() {
        return Boolean.TRUE.equals(mViewModel.getLoadMoreInFlight().getValue());
    }

    @Override
    public int getFontSizeTitle() {
        return mFontSizeTitle;
    }

    @Override
    public int getFontSizeDetails() {
        return mFontSizeDetails;
    }

    @Override
    public int getTitleColor() {
        return mTitleColor;
    }

    @Override
    public int getTitleReadColor() {
        return mTitleReadColor;
    }

    @Override
    public void onOpenComments(HNPost post) {
        Intent i = new Intent(MainActivity.this, CommentsActivity_.class);
        i.putExtra(CommentsActivity.EXTRA_HNPOST, post);
        startActivity(i);
    }

    @Override
    public void onPostClicked(HNPost post) {
        mViewModel.markRead(post);
        if (post.getURLDomain().equals(HACKERNEWS_URLDOMAIN)) {
            onOpenComments(post);
        } else if (Settings.getHtmlViewer(MainActivity.this).equals(
                getString(R.string.pref_htmlviewer_browser))) {
            openURLInBrowser(getArticleViewURL(post), MainActivity.this);
        } else {
            openPostInApp(post, null, MainActivity.this);
        }
    }

    @Override
    public void onLongPress(HNPost post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LongPressMenuListAdapter adapter = new LongPressMenuListAdapter(post);
        builder.setAdapter(adapter, adapter).show();
    }

    @Override
    public void onLoadMore() {
        mViewModel.loadMore();
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

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mListState = state.getParcelable(LIST_STATE);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        mListState = mPostsList.onSaveInstanceState();
        state.putParcelable(LIST_STATE, mListState);
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
                    || (mPost.getUpvoteURL(Settings.getUserName(MainActivity.this)) != null
                            && !mViewModel.isUpvoted(mPost));

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
        public void registerDataSetObserver(android.database.DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(android.database.DataSetObserver observer) {
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
                        mViewModel.vote(mPost.getUpvoteURL(Settings
                                .getUserName(MainActivity.this)), mPost);
                    }
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                openPostInApp(mPost, getItem(item).toString(),
                        MainActivity.this);
                mViewModel.markRead(mPost);
                break;
            case 5:
                openURLInBrowser(getArticleViewURL(mPost), MainActivity.this);
                mViewModel.markRead(mPost);
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

    public static void shareUrl(HNPost post, Activity a) {
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

}
