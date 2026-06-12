package com.manuelmaly.hn.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.model.HNComment;
import com.manuelmaly.hn.model.HNPostComments;
import com.manuelmaly.hn.reuse.LinkifiedTextView;

/**
 * Adapter for the comments list in CommentsActivity.
 * Extracted from CommentsActivity.CommentsAdapter.
 *
 * Delegates comment expand/collapse and long-press menu actions
 * to the {@link Callbacks} interface, decoupling from the Activity.
 */
public class CommentsAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final Callbacks callbacks;

    private HNPostComments comments;
    private int commentLevelIndentPx;
    private int fontSizeText;
    private int fontSizeMetadata;

    /**
     * Callback interface for comment interactions.
     */
    public interface Callbacks {
        /** Called when a comment with children is tapped to toggle expand/collapse. */
        void onToggleCommentExpanded(HNComment comment);
        /** Called when a comment is long-pressed to show the context menu. */
        void onCommentLongPress(HNComment comment);
    }

    public CommentsAdapter(LayoutInflater inflater, Callbacks callbacks) {
        this.inflater = inflater;
        this.callbacks = callbacks;
    }

    /**
     * Updates the data and display parameters for the comments list.
     */
    public void updateData(HNPostComments comments, int commentLevelIndentPx,
                           int fontSizeText, int fontSizeMetadata) {
        this.comments = comments;
        this.commentLevelIndentPx = commentLevelIndentPx;
        this.fontSizeText = fontSizeText;
        this.fontSizeMetadata = fontSizeMetadata;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return comments != null ? comments.getComments().size() : 0;
    }

    @Override
    public HNComment getItem(int position) {
        return comments.getComments().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CommentViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.comments_list_item, null);
            holder = new CommentViewHolder();
            holder.rootView = convertView;
            holder.textView = (LinkifiedTextView) convertView
                    .findViewById(R.id.comments_list_item_text);
            holder.spacersContainer = (LinearLayout) convertView
                    .findViewById(R.id.comments_list_item_spacerscontainer);
            holder.authorView = (TextView) convertView
                    .findViewById(R.id.comments_list_item_author);
            holder.timeAgoView = (TextView) convertView
                    .findViewById(R.id.comments_list_item_timeago);
            holder.expandView = (ImageView) convertView
                    .findViewById(R.id.comments_list_item_expand);

            // Set click listeners once; use rootView.getTag() to retrieve holder at click time
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Find the root convertView by traversing up
                    View current = v;
                    while (current != null && current.getTag() == null) {
                        if (current.getParent() instanceof View) {
                            current = (View) current.getParent();
                        } else {
                            break;
                        }
                    }
                    CommentViewHolder h = (current != null) ? (CommentViewHolder) current.getTag() : null;
                    if (h != null && h.currentComment != null
                            && h.currentComment.getTreeNode() != null
                            && h.currentComment.getTreeNode().hasChildren()) {
                        callbacks.onToggleCommentExpanded(h.currentComment);
                    }
                }
            };
            holder.rootView.setOnClickListener(clickListener);
            holder.textView.setOnClickListener(clickListener);

            View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    View current = v;
                    while (current != null && current.getTag() == null) {
                        if (current.getParent() instanceof View) {
                            current = (View) current.getParent();
                        } else {
                            break;
                        }
                    }
                    CommentViewHolder h = (current != null) ? (CommentViewHolder) current.getTag() : null;
                    if (h != null && h.currentComment != null) {
                        callbacks.onCommentLongPress(h.currentComment);
                    }
                    return true;
                }
            };
            holder.rootView.setOnLongClickListener(longClickListener);
            holder.textView.setOnLongClickListener(longClickListener);

            convertView.setTag(holder);
        }

        holder = (CommentViewHolder) convertView.getTag();
        HNComment comment = getItem(position);
        holder.currentComment = comment;
        holder.setComment(comment, commentLevelIndentPx,
                convertView.getContext(), fontSizeText, fontSizeMetadata);

        return convertView;
    }

    /**
     * ViewHolder for a single comment item.
     * Extracted from CommentsActivity.CommentViewHolder.
     */
    public static class CommentViewHolder {
        View rootView;
        LinkifiedTextView textView;
        TextView authorView;
        TextView timeAgoView;
        ImageView expandView;
        LinearLayout spacersContainer;

        HNComment currentComment;

        public void setComment(HNComment comment, int commentLevelIndentPx,
                               Context c, int commentTextSize, int metadataTextSize) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, commentTextSize);
            textView.setTextColor(comment.getColor());
            textView.setLinkTextColor(comment.getColor());
            textView.setText(Html.fromHtml(comment.getText()));
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            authorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, metadataTextSize);
            timeAgoView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, metadataTextSize);
            if (!TextUtils.isEmpty(comment.getAuthor())) {
                authorView.setText(comment.getAuthor());
                timeAgoView.setText(", " + comment.getTimeAgo());
            } else {
                authorView.setText(c.getString(R.string.deleted));
                timeAgoView.setText("");
            }
            expandView.setVisibility(comment.getTreeNode().isExpanded()
                    ? View.INVISIBLE : View.VISIBLE);
            spacersContainer.removeAllViews();
            for (int i = 0; i < comment.getCommentLevel(); i++) {
                View spacer = new View(c);
                spacer.setLayoutParams(new LinearLayout.LayoutParams(
                        commentLevelIndentPx, LayoutParams.MATCH_PARENT));
                int spacerAlpha = Math.max(70 - i * 10, 10);
                spacer.setBackgroundColor(Color.argb(spacerAlpha, 0, 0, 0));
                spacersContainer.addView(spacer, i);
            }
        }
    }
}
