package com.manuelmaly.hn;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.MenuItemCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.manuelmaly.hn.adapter.PostLongPressAdapter;
import com.manuelmaly.hn.adapter.PostsAdapter;
import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.server.HNCredentials;
import com.manuelmaly.hn.task.HNVoteTask;
import com.manuelmaly.hn.task.ITaskFinishedHandler;
import com.manuelmaly.hn.util.FontHelper;
import com.manuelmaly.hn.util.FontSizeHelper;
import com.manuelmaly.hn.util.NavigationHelper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Main feed screen using MVVM architecture.
 *
 * Data loading, state management, and business logic are handled by {@link com.manuelmaly.hn.viewmodel.MainViewModel}.
 * This Activity is responsible only for:
 * - UI setup and view binding
 * - Observing ViewModel LiveData for display updates
 * - Forwarding user interactions to the ViewModel
 * - Navigation and menu handling
 */
@EActivity(R.layout.main)
public class MainActivity extends BaseListActivity {

    private static final int TASKCODE_VOTE = 100;
    private static final String LIST_STATE = "listState";

    @ViewById(R.id.main_list)
    ListView mPostsList;

    @ViewById(R.id.main_root)
    LinearLayout mRootView;

    @ViewById(R.id.main_swiperefreshlayout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @SystemService
    LayoutInflater mInflater;

    private com.manuelmaly.hn.viewmodel.MainViewModel viewModel;
    private PostsAdapter adapter;
    private TextView emptyListPlaceholder;

    // UI-only state (not data state)
    private String currentFontSize;
    private int fontSizeTitle;
    private int fontSizeDetails;
    private int titleColor;
    private int titleReadColor;
    private Parcelable listState;
    private boolean shouldShowRefreshing;

    // Vote state (uses legacy task flow, result recorded in ViewModel)
    private Set<HNPost> upvotedPosts = new HashSet<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure overflow menu icon visibility
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class
                    .getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            // not relevant on newer devices
        }

        TextView tv = (TextView) getSupportActionBar().getCustomView()
                .findViewById(R.id.actionbar_title);
        tv.setTypeface(FontHelper.getComfortaa(this, true));
    }

    @AfterViews
    public void init() {
        viewModel = new ViewModelProvider(this).get(
                com.manuelmaly.hn.viewmodel.MainViewModel.class);

        adapter = new PostsAdapter(mInflater, adapterCallbacks);
        emptyListPlaceholder = getEmptyTextView(mRootView);
        emptyListPlaceholder.setTypeface(FontHelper.getComfortaa(this, true));
        mPostsList.setEmptyView(emptyListPlaceholder);
        mPostsList.setAdapter(adapter);

        titleColor = getResources().getColor(R.color.dark_gray_post_title);
        titleReadColor = getResources().getColor(R.color.gray_post_title_read);

        toggleSwipeRefreshLayout();
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                viewModel.refresh();
            }
        });

        // Observe ViewModel LiveData
        viewModel.getFeedStatus().observe(this, resource -> {
            if (resource.status == Resource.Status.LOADING) {
                setShowRefreshing(true);
            } else {
                setShowRefreshing(false);
                if (resource.status == Resource.Status.ERROR) {
                    Toast.makeText(this,
                            getString(R.string.error_unable_to_retrieve_feed),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getPostList().observe(this, posts -> {
            pushAdapterData();
        });

        viewModel.getLoadMoreResult().observe(this, resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                HNFeed currentFeed = viewModel.getCurrentFeed().getValue();
                if (currentFeed != null && resource.data != null) {
                    if (resource.data.getPosts() == null || resource.data.getPosts().size() == 0) {
                        currentFeed.setLoadedMore(true);
                    }
                    currentFeed.appendLoadMoreFeed(resource.data);
                    viewModel.updateCurrentFeed(currentFeed);
                    pushAdapterData();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(this,
                        getString(R.string.error_unable_to_load_more),
                        Toast.LENGTH_SHORT).show();
                HNFeed currentFeed = viewModel.getCurrentFeed().getValue();
                if (currentFeed != null) {
                    currentFeed.setLoadedMore(true);
                }
            }
        });

        viewModel.getUpvotedPosts().observe(this, voted -> {
            upvotedPosts = voted != null ? voted : new HashSet<>();
        });

        // Load data
        viewModel.loadAlreadyReadCache();
        viewModel.loadCachedFeed();
        viewModel.refresh();
    }

    // --- Adapter Callbacks ---

    private final PostsAdapter.Callbacks adapterCallbacks = new PostsAdapter.Callbacks() {
        @Override
        public void onPostClick(HNPost post) {
            viewModel.markAsRead(post);
            NavigationHelper.openPost(post, MainActivity.this);
        }

        @Override
        public void onPostLongPress(HNPost post) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            PostLongPressAdapter longPressAdapter = new PostLongPressAdapter(
                    MainActivity.this, mInflater, post, upvotedPosts, longPressCallbacks);
            builder.setAdapter(longPressAdapter, longPressAdapter).show();
        }

        @Override
        public void onCommentsClick(HNPost post) {
            NavigationHelper.openComments(post, MainActivity.this);
        }

        @Override
        public void onLoadMoreClick() {
            setShowRefreshing(true);
            viewModel.loadMore();
        }
    };

    private final PostLongPressAdapter.Callbacks longPressCallbacks = new PostLongPressAdapter.Callbacks() {
        @Override
        public void onUpvote(HNPost post) {
            String voteURL = post.getUpvoteURL(Settings.getUserName(MainActivity.this));
            if (voteURL != null) {
                HNVoteTask.start(voteURL, MainActivity.this,
                        new VoteTaskFinishedHandler(), TASKCODE_VOTE, post);
            }
        }

        @Override
        public void onOpenInApp(HNPost post, String htmlProvider) {
            viewModel.markAsRead(post);
            NavigationHelper.openPostInApp(post, htmlProvider, MainActivity.this);
        }

        @Override
        public void onOpenInBrowser(HNPost post) {
            viewModel.markAsRead(post);
            NavigationHelper.openURLInBrowser(
                    NavigationHelper.getArticleViewURL(post, MainActivity.this),
                    MainActivity.this);
        }

        @Override
        public void onShare(HNPost post) {
            NavigationHelper.shareUrl(post, MainActivity.this);
        }
    };

    // --- Vote callback (legacy task flow, result recorded in ViewModel) ---

    class VoteTaskFinishedHandler implements ITaskFinishedHandler<Boolean> {
        @Override
        public void onTaskFinished(int taskCode,
                ITaskFinishedHandler.TaskResultCode code,
                Boolean result, Object tag) {
            if (taskCode == TASKCODE_VOTE) {
                if (result != null && result.booleanValue()) {
                    Toast.makeText(MainActivity.this, R.string.vote_success,
                            Toast.LENGTH_SHORT).show();
                    HNPost post = (HNPost) tag;
                    if (post != null) {
                        viewModel.recordUpvote(post);
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.vote_error,
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // --- Lifecycle ---

    @Override
    protected void onResume() {
        super.onResume();

        // Reload feed if user changed
        HNFeed currentFeed = viewModel.getCurrentFeed().getValue();
        boolean registeredUserChanged = currentFeed != null
                && currentFeed.getUserAcquiredFor() != null
                && !currentFeed.getUserAcquiredFor().equals(Settings.getUserName(this));

        if (HNCredentials.isInvalidated() || registeredUserChanged) {
            viewModel.refresh();
        }

        if (refreshFontSizes()) {
            pushAdapterData();
        }

        if (listState != null) {
            mPostsList.onRestoreInstanceState(listState);
        }
        listState = null;

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
        if (!shouldShowRefreshing) {
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
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.menu_about:
            startActivity(new Intent(this, AboutActivity_.class));
            return true;
        case R.id.menu_refresh:
            viewModel.refresh();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        listState = state.getParcelable(LIST_STATE);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        listState = mPostsList.onSaveInstanceState();
        state.putParcelable(LIST_STATE, listState);
    }

    // --- UI Helpers ---

    private void toggleSwipeRefreshLayout() {
        mSwipeRefreshLayout.setEnabled(Settings.isPullDownRefresh(this));
    }

    private boolean refreshFontSizes() {
        final String fontSize = Settings.getFontSize(this);
        if (currentFontSize == null || !currentFontSize.equals(fontSize)) {
            currentFontSize = fontSize;
            FontSizeHelper.FeedFontSizes sizes = FontSizeHelper.getFeedFontSizes(this);
            fontSizeTitle = sizes.titleSize;
            fontSizeDetails = sizes.detailsSize;
            return true;
        }
        return false;
    }

    private void pushAdapterData() {
        HNFeed feed = viewModel.getCurrentFeed().getValue();
        Set<Integer> alreadyRead = new HashSet<>();
        if (viewModel.getPostList().getValue() != null) {
            for (com.manuelmaly.hn.viewmodel.PostUiModel model : viewModel.getPostList().getValue()) {
                if (model.isRead) {
                    alreadyRead.add(model.post.getTitle().hashCode());
                }
            }
        }
        adapter.updateData(feed, alreadyRead,
                fontSizeTitle, fontSizeDetails,
                titleColor, titleReadColor,
                viewModel.isLoadMoreRunning());
    }

    private void setShowRefreshing(boolean showRefreshing) {
        if (!Settings.isPullDownRefresh(this)) {
            shouldShowRefreshing = showRefreshing;
            supportInvalidateOptionsMenu();
        }
        if (mSwipeRefreshLayout.isEnabled()
                && (!mSwipeRefreshLayout.isRefreshing() || !showRefreshing)) {
            mSwipeRefreshLayout.setRefreshing(showRefreshing);
        }
    }
}
