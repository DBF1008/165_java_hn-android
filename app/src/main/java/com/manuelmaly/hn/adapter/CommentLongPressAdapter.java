package com.manuelmaly.hn.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.Settings;
import com.manuelmaly.hn.model.HNComment;
import com.manuelmaly.hn.model.HNCommentTreeNode;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Adapter for the long-press context menu on a comment item.
 * Extracted from CommentsActivity.LongPressMenuListAdapter.
 *
 * Implements both {@link ListAdapter} (for AlertDialog) and
 * {@link android.content.DialogInterface.OnClickListener} (for item selection).
 */
public class CommentLongPressAdapter implements ListAdapter, DialogInterface.OnClickListener {

    private final HNComment comment;
    private final boolean isLoggedIn;
    private final boolean upVotingEnabled;
    private final boolean downVotingEnabled;
    private final ArrayList<CharSequence> items;
    private final Context context;
    private final LayoutInflater inflater;
    private final Callbacks callbacks;

    /**
     * Callback interface for menu item actions.
     */
    public interface Callbacks {
        void onUpvote(HNComment comment);
        void onDownvote(HNComment comment);
        void onCollapseThread(HNComment comment);
        void onCollapseComment(HNComment comment);
        void onExpandComment(HNComment comment);
        void onLoginRequired();
    }

    public CommentLongPressAdapter(Context context, LayoutInflater inflater,
                                   HNComment comment, HashSet<HNComment> votedComments,
                                   Callbacks callbacks) {
        this.context = context;
        this.inflater = inflater;
        this.comment = comment;
        this.callbacks = callbacks;

        String userName = Settings.getUserName(context);
        isLoggedIn = Settings.isUserLoggedIn(context);
        upVotingEnabled = !isLoggedIn
                || (comment.getUpvoteUrl(userName) != null && !votedComments.contains(comment));
        downVotingEnabled = isLoggedIn
                && (comment.getDownvoteUrl(userName) != null && !votedComments.contains(comment));

        items = new ArrayList<CharSequence>();

        if (upVotingEnabled) {
            items.add(context.getString(R.string.upvote));
        }
        if (downVotingEnabled) {
            items.add(context.getString(R.string.downvote));
        }
        if (!upVotingEnabled && !downVotingEnabled) {
            items.add(context.getString(R.string.already_voted_on));
        }

        if (comment.getTreeNode().isExpanded()) {
            items.add(context.getString(R.string.collapse_comment));
        } else {
            items.add(context.getString(R.string.expand_comment));
        }

        if (comment.getTreeNode().getParent() != null) {
            items.add(context.getString(R.string.collapse_thread));
        }
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public CharSequence getItem(int position) {
        return items.get(position);
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
        TextView view = (TextView) inflater.inflate(
                android.R.layout.simple_list_item_1, null);
        view.setText(getItem(position));
        if (!upVotingEnabled && position == 0) {
            view.setTextColor(context.getResources().getColor(
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
    public void registerDataSetObserver(DataSetObserver observer) {}

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {}

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        if (!upVotingEnabled && position == 0) {
            return false;
        }
        return true;
    }

    @Override
    public void onClick(DialogInterface dialog, int item) {
        String clickedText = getItem(item).toString();

        if (clickedText.equals(context.getString(R.string.upvote))) {
            if (!isLoggedIn) {
                callbacks.onLoginRequired();
            } else {
                callbacks.onUpvote(comment);
            }
        } else if (clickedText.equals(context.getString(R.string.downvote))) {
            callbacks.onDownvote(comment);
        } else if (clickedText.equals(context.getString(R.string.collapse_thread))) {
            callbacks.onCollapseThread(comment);
        } else {
            // collapse_comment or expand_comment
            if (comment.getTreeNode().isExpanded()) {
                callbacks.onCollapseComment(comment);
            } else {
                callbacks.onExpandComment(comment);
            }
        }
    }
}
