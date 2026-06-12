package com.manuelmaly.hn;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Html;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.MenuItemCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.manuelmaly.hn.adapter.CommentLongPressAdapter;
import com.manuelmaly.hn.adapter.CommentsAdapter;
import com.manuelmaly.hn.data.Resource;
import com.manuelmaly.hn.login.LoginActivity_;
import com.manuelmaly.hn.model.HNComment;
import com.manuelmaly.hn.model.HNCommentTreeNode;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.model.HNPostComments;
import com.manuelmaly.hn.task.HNVoteTask;
import com.manuelmaly.hn.task.ITaskFinishedHandler;
import com.manuelmaly.hn.util.DisplayHelper;
import com.manuelmaly.hn.util.FontHelper;
import com.manuelmaly.hn.util.FontSizeHelper;
import com.manuelmaly.hn.util.NavigationHelper;
import com.manuelmaly.hn.util.SpotlightActivity;
import com.manuelmaly.hn.util.ViewedUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import java.util.HashSet;

/**
 * Comments screen using MVVM architecture.
 *
 * Data loading, comment tree state, and vote tracking are handled by
 * {@link com.manuelmaly.hn.viewmodel.CommentsViewModel}.
 * This Activity is responsible only for:
 * - UI setup and view binding
 * - Observing ViewModel LiveData for display updates
 * - Forwarding user interactions to the ViewModel
 * - Login flow and spotlight onboarding
 */
@EActivity(R.layout.comments_activity)
public class CommentsActivity extends BaseListActivity {

    public static final String EXTRA_HNPOST = "HNPOST";
    private static final int TASKCODE_VOTE = 100;
    private static final int ACTIVITY_LOGIN = 136;
    private static final int ACTIVITY_SPOTLIGHT = 137;
    private static final String LIST_STATE = "listState";

    @ViewById(R.id.comments_list)
    ListView mCommentsList;

    @ViewById(R.id.comments_root)
    LinearLayout mRootView;

    @ViewById(R.id.comments_swiperefreshlayout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @SystemService
    LayoutInflater mInflater;

    private com.manuelmaly.hn.viewmodel.CommentsViewModel viewModel;
    private CommentsAdapter commentsAdapter;

    // UI-only state
    private HNPost post;
    private LinearLayout commentHeader;
    private TextView commentHeaderText;
    private TextView emptyView;
    private TextView actionbarTitle;
    private int commentLevelIndentPx;
    private String currentFontSize;
    private int fontSizeText;
    private int fontSizeMetadata;
    private Parcelable listState;
    private boolean shouldShowRefreshing;
    private boolean haveLoadedPosts;

    @AfterViews
    public void init() {
        post = (HNPost) getIntent().getSerializableExtra(EXTRA_HNPOST);
        if (post == null || post.getPostID() == null) {
            Toast.makeText(this, "The belonging post has not been loaded",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(
                com.manuelmaly.hn.viewmodel.CommentsViewModel.class);

        commentLevelIndentPx = Math.min(DisplayHelper.getScreenHeight(this),
                DisplayHelper.getScreenWidth(this)) / 30;

        initCommentsHeader();
        commentsAdapter = new CommentsAdapter(mInflater, commentsCallbacks);
        emptyView = getEmptyTextView(mRootView);
        mCommentsList.setEmptyView(emptyView);
        mCommentsList.addHeaderView(commentHeader, null, false);
        mCommentsList.setAdapter(commentsAdapter);

        actionbarTitle = (TextView) getSupportActionBar().getCustomView()
                .findViewById(R.id.actionbar_title);
        actionbarTitle.setTypeface(FontHelper.getComfortaa(this, true));
        actionbarTitle.setText(getString(R.string.comments));
        actionbarTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Settings.getHtmlViewer(CommentsActivity.this).equals(
                        getString(R.string.pref_htmlviewer_browser))) {
                    String articleURL = ArticleReaderActivity.getArticleViewURL(
                            post, Settings.getHtmlProvider(CommentsActivity.this),
                            CommentsActivity.this);
                    NavigationHelper.openURLInBrowser(articleURL, CommentsActivity.this);
                } else {
                    openArticleReader();
                }
            }
        });

        toggleSwipeRefreshLayout();
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                viewModel.loadComments(post.getPostID());
            }
        });

        // Observe ViewModel LiveData
        viewModel.getCommentsStatus().observe(this, resource -> {
            if (resource.status == Resource.Status.LOADING) {
                setShowRefreshing(true);
            } else {
                setShowRefreshing(false);
                if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                    showComments(resource.data);
                } else if (resource.status == Resource.Status.ERROR) {
                    Toast.makeText(this,
                            getString(R.string.error_unable_to_retrieve_comments),
                            Toast.LENGTH_SHORT).show();
                }
                updateEmptyView();
            }
        });

        viewModel.getVotedComments().observe(this, voted -> {
            // Voted state is now tracked by ViewModel, adapter will be refreshed
            // via the flat comments list
        });

        viewModel.getFlatComments().observe(this, comments -> {
            pushAdapterData();
        });

        // Load data
        viewModel.loadCachedComments(post.getPostID());
        viewModel.loadComments(post.getPostID());
    }

    // --- Adapter Callbacks ---

    private final CommentsAdapter.Callbacks commentsCallbacks = new CommentsAdapter.Callbacks() {
        @Override
        public void onToggleCommentExpanded(HNComment comment) {
            viewModel.toggleCommentExpanded(comment);
        }

        @Override
        public void onCommentLongPress(HNComment comment) {
            HashSet<HNComment> voted = viewModel.getVotedComments().getValue();
            if (voted == null) voted = new HashSet<>();

            AlertDialog.Builder builder = new AlertDialog.Builder(CommentsActivity.this);
            CommentLongPressAdapter longPressAdapter = new CommentLongPressAdapter(
                    CommentsActivity.this, mInflater, comment, voted, longPressCallbacks);
            builder.setAdapter(longPressAdapter, longPressAdapter);
            Dialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        }
    };

    private final CommentLongPressAdapter.Callbacks longPressCallbacks = new CommentLongPressAdapter.Callbacks() {
        @Override
        public void onUpvote(HNComment comment) {
            String voteURL = comment.getUpvoteUrl(Settings.getUserName(CommentsActivity.this));
            if (voteURL != null) {
                HNVoteTask.start(voteURL, CommentsActivity.this,
                        new VoteTaskFinishedHandler(), TASKCODE_VOTE, comment);
            }
        }

        @Override
        public void onDownvote(HNComment comment) {
            String voteURL = comment.getDownvoteUrl(Settings.getUserName(CommentsActivity.this));
            if (voteURL != null) {
                HNVoteTask.start(voteURL, CommentsActivity.this,
                        new VoteTaskFinishedHandler(), TASKCODE_VOTE, comment);
            }
        }

        @Override
        public void onCollapseThread(HNComment comment) {
            viewModel.toggleThreadExpanded(comment);
        }

        @Override
        public void onCollapseComment(HNComment comment) {
            viewModel.toggleCommentExpanded(comment);
        }

        @Override
        public void onExpandComment(HNComment comment) {
            viewModel.toggleCommentExpanded(comment);
        }

        @Override
        public void onLoginRequired() {
            viewModel.setPendingVote(
                    viewModel.getPendingVote().getValue()); // preserve current pending
            startActivityForResult(new Intent(getApplicationContext(),
                    LoginActivity_.class), ACTIVITY_LOGIN);
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
                    Toast.makeText(CommentsActivity.this,
                            R.string.vote_success, Toast.LENGTH_SHORT).show();
                    HNComment comment = (HNComment) tag;
                    if (comment != null) {
                        viewModel.recordVote(comment);
                        pushAdapterData();
                    }
                } else {
                    Toast.makeText(CommentsActivity.this, R.string.vote_error,
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // --- Lifecycle ---

    @Override
    protected void onResume() {
        super.onResume();

        if (refreshFontSizes()) {
            pushAdapterData();
        }

        if (listState != null) {
            mCommentsList.onRestoreInstanceState(listState);
        }
        listState = null;

        if (!ViewedUtils.getActivityViewed(this)) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showCommentsSpotlight();
                    ViewedUtils.setActivityViewed(CommentsActivity.this);
                }
            }, 250);
        }

        toggleSwipeRefreshLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share_refresh, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem refreshItem = menu.findItem(R.id.menu_refresh);
        if (shouldShowRefreshing) {
            View refreshView = mInflater.inflate(R.layout.refresh_icon, null);
            MenuItemCompat.setActionView(refreshItem, refreshView);
        } else {
            MenuItemCompat.setActionView(refreshItem, null);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_refresh:
            viewModel.loadComments(post.getPostID());
            return true;
        case android.R.id.home:
            finish();
            return true;
        case R.id.menu_share:
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, post.getTitle()
                    + " | Hacker News");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "https://news.ycombinator.com/item?id=" + post.getPostID());
            startActivity(Intent.createChooser(shareIntent,
                    getString(R.string.share_comments_url)));
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
        listState = mCommentsList.onSaveInstanceState();
        state.putParcelable(LIST_STATE, listState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case ACTIVITY_LOGIN:
            if (resultCode == RESULT_OK) {
                HNComment pending = viewModel.getPendingVote().getValue();
                if (pending != null) {
                    viewModel.updateComments(new HNPostComments());
                    viewModel.loadComments(post.getPostID());
                    Toast.makeText(this,
                            getString(R.string.login_success_reloading),
                            Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this,
                        getString(R.string.error_login_to_vote),
                        Toast.LENGTH_LONG).show();
            }
            break;
        case ACTIVITY_SPOTLIGHT:
            if (resultCode == RESULT_OK) {
                openArticleReader();
            }
            break;
        }
    }

    // --- UI Helpers ---

    private void showComments(HNPostComments comments) {
        if (comments.getHeaderHtml() != null
                && commentHeaderText.getVisibility() != View.VISIBLE) {
            commentHeaderText.setVisibility(View.VISIBLE);
            commentHeaderText.setText(Html.fromHtml(comments.getHeaderHtml())
                    .toString().trim());
            Linkify.addLinks(commentHeaderText, Linkify.WEB_URLS);
        }
        viewModel.updateComments(comments);
    }

    private void initCommentsHeader() {
        if (commentHeader == null) {
            commentHeader = new LinearLayout(this);
            commentHeader.setOrientation(LinearLayout.VERTICAL);
            commentHeaderText = new TextView(this);
            commentHeader.addView(commentHeaderText);
            commentHeaderText.setPadding(commentLevelIndentPx,
                    commentLevelIndentPx / 2, commentLevelIndentPx / 2,
                    commentLevelIndentPx / 2);
            commentHeaderText.setTextColor(getResources().getColor(
                    R.color.gray_comments_information));
            View v = new View(this);
            v.setBackgroundColor(getResources().getColor(
                    R.color.gray_comments_divider));
            v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
            commentHeader.addView(v);
            commentHeaderText.setVisibility(View.GONE);
        }
    }

    private void toggleSwipeRefreshLayout() {
        mSwipeRefreshLayout.setEnabled(Settings.isPullDownRefresh(this));
    }

    private boolean refreshFontSizes() {
        final String fontSize = Settings.getFontSize(this);
        if (currentFontSize == null || !currentFontSize.equals(fontSize)) {
            currentFontSize = fontSize;
            FontSizeHelper.CommentFontSizes sizes = FontSizeHelper.getCommentFontSizes(this);
            fontSizeText = sizes.textSize;
            fontSizeMetadata = sizes.metadataSize;
            return true;
        }
        return false;
    }

    private void updateEmptyView() {
        if (haveLoadedPosts) {
            emptyView.setText(getString(R.string.no_comments));
        }
        haveLoadedPosts = true;
    }

    private void pushAdapterData() {
        HNPostComments comments = viewModel.getCurrentComments().getValue();
        if (comments == null) comments = new HNPostComments();
        commentsAdapter.updateData(comments, commentLevelIndentPx,
                fontSizeText, fontSizeMetadata);
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

    private void showCommentsSpotlight() {
        int[] posArray = new int[2];
        actionbarTitle.getLocationInWindow(posArray);
        Intent intent = SpotlightActivity.intentForSpotlightActivity(
                this, posArray[0], actionbarTitle.getWidth(),
                0, getSupportActionBar().getHeight(),
                getString(R.string.click_on_comments));
        startActivityForResult(intent, ACTIVITY_SPOTLIGHT);
        overridePendingTransition(android.R.anim.fade_in,
                android.R.anim.fade_out);
    }

    private void openArticleReader() {
        Intent intent = new Intent(this, ArticleReaderActivity_.class);
        intent.putExtra(EXTRA_HNPOST, post);
        if (getIntent().getStringExtra(
                ArticleReaderActivity.EXTRA_HTMLPROVIDER_OVERRIDE) != null) {
            intent.putExtra(ArticleReaderActivity.EXTRA_HTMLPROVIDER_OVERRIDE,
                    getIntent().getStringExtra(
                            ArticleReaderActivity.EXTRA_HTMLPROVIDER_OVERRIDE));
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,
                android.R.anim.fade_out);
        finish();
    }
}
