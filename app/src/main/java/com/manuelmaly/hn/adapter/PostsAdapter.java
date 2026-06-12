package com.manuelmaly.hn.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.parser.BaseHTMLParser;
import com.manuelmaly.hn.util.FontHelper;

import java.util.Set;

/**
 * Adapter for the main feed list in MainActivity.
 * Extracted from MainActivity.PostsAdapter.
 *
 * Supports two view types: regular post items and a "Load More" footer.
 * Delegates all click interactions to the {@link Callbacks} interface.
 */
public class PostsAdapter extends BaseAdapter {

    private static final int VIEWTYPE_POST = 0;
    private static final int VIEWTYPE_LOADMORE = 1;

    private final LayoutInflater inflater;
    private final Callbacks callbacks;

    private HNFeed feed;
    private Set<Integer> alreadyRead;
    private int fontSizeTitle;
    private int fontSizeDetails;
    private int titleColor;
    private int titleReadColor;
    private boolean isLoadMoreRunning;

    /**
     * Callback interface for post interactions.
     */
    public interface Callbacks {
        /** Called when a post's text area is tapped (open article/comments). */
        void onPostClick(HNPost post);
        /** Called when a post is long-pressed (show context menu). */
        void onPostLongPress(HNPost post);
        /** Called when a post's comments button is tapped. */
        void onCommentsClick(HNPost post);
        /** Called when the "Load More" footer is tapped. */
        void onLoadMoreClick();
    }

    public PostsAdapter(LayoutInflater inflater, Callbacks callbacks) {
        this.inflater = inflater;
        this.callbacks = callbacks;
    }

    /**
     * Updates all display data and refreshes the list.
     */
    public void updateData(HNFeed feed, Set<Integer> alreadyRead,
                           int fontSizeTitle, int fontSizeDetails,
                           int titleColor, int titleReadColor,
                           boolean isLoadMoreRunning) {
        this.feed = feed;
        this.alreadyRead = alreadyRead;
        this.fontSizeTitle = fontSizeTitle;
        this.fontSizeDetails = fontSizeDetails;
        this.titleColor = titleColor;
        this.titleReadColor = titleReadColor;
        this.isLoadMoreRunning = isLoadMoreRunning;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (feed == null || feed.getPosts().size() == 0) {
            return 0;
        }
        return feed.getPosts().size() + (feed.isLoadedMore() ? 0 : 1);
    }

    @Override
    public HNPost getItem(int position) {
        if (getItemViewType(position) == VIEWTYPE_POST) {
            return feed.getPosts().get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (feed != null && position < feed.getPosts().size()) {
            return VIEWTYPE_POST;
        }
        return VIEWTYPE_LOADMORE;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case VIEWTYPE_POST:
                return getPostView(convertView, parent, position);
            case VIEWTYPE_LOADMORE:
                return getLoadMoreView(parent);
            default:
                return convertView;
        }
    }

    private View getPostView(View convertView, ViewGroup parent, final int position) {
        PostViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.main_list_item, null);
            holder = new PostViewHolder(convertView);
            holder.commentsButton.setTypeface(FontHelper.getComfortaa(
                    convertView.getContext(), false));
            holder.pointsView.setTypeface(FontHelper.getComfortaa(
                    convertView.getContext(), true));

            // Set click listeners once, using holder.currentPost for live reference
            holder.textContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewGroup container = (ViewGroup) v;
                    PostViewHolder h = (PostViewHolder) container.getTag();
                    if (h != null && h.currentPost != null) {
                        callbacks.onPostClick(h.currentPost);
                    }
                }
            });
            holder.textContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ViewGroup container = (ViewGroup) v;
                    PostViewHolder h = (PostViewHolder) container.getTag();
                    if (h != null && h.currentPost != null) {
                        callbacks.onPostLongPress(h.currentPost);
                    }
                    return true;
                }
            });
            holder.commentsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewGroup container = (ViewGroup) v.getParent();
                    PostViewHolder h = (PostViewHolder) container.getTag();
                    if (h != null && h.currentPost != null) {
                        callbacks.onCommentsClick(h.currentPost);
                    }
                }
            });

            convertView.setTag(holder);
        }

        holder = (PostViewHolder) convertView.getTag();
        HNPost item = getItem(position);
        holder.currentPost = item;

        // Bind data
        holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeTitle);
        holder.titleView.setText(item.getTitle());
        holder.titleView.setTextColor(isRead(item) ? titleReadColor : titleColor);

        holder.urlView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeDetails);
        holder.urlView.setText(item.getURLDomain());

        holder.pointsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeDetails);
        if (item.getPoints() != BaseHTMLParser.UNDEFINED) {
            holder.pointsView.setText(item.getPoints() + "");
        } else {
            holder.pointsView.setText("-");
        }

        holder.commentsButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeTitle);
        if (item.getCommentsCount() != BaseHTMLParser.UNDEFINED) {
            holder.commentsButton.setVisibility(View.VISIBLE);
            holder.commentsButton.setText(item.getCommentsCount() + "");
        } else {
            holder.commentsButton.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private View getLoadMoreView(ViewGroup parent) {
        // Always re-inflate the load more view (only one instance)
        View convertView = inflater.inflate(R.layout.main_list_item_loadmore, null);
        final TextView textView = (TextView) convertView
                .findViewById(R.id.main_list_item_loadmore_text);
        textView.setTypeface(FontHelper.getComfortaa(convertView.getContext(), true));
        final ImageView imageView = (ImageView) convertView
                .findViewById(R.id.main_list_item_loadmore_loadingimage);

        if (isLoadMoreRunning) {
            textView.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            convertView.setClickable(false);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.VISIBLE);
                v.setClickable(false);
                callbacks.onLoadMoreClick();
            }
        });

        return convertView;
    }

    private boolean isRead(HNPost post) {
        return alreadyRead != null && alreadyRead.contains(post.getTitle().hashCode());
    }
}
