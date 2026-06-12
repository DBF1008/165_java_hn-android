package com.manuelmaly.hn.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.parser.BaseHTMLParser;
import com.manuelmaly.hn.util.FontHelper;

/**
 * Feed list adapter, extracted from {@code MainActivity}. It renders rows and a
 * trailing "load more" cell, but holds no data or business logic itself: feed
 * contents, read-state, font sizes/colors and the load-more in-flight flag are
 * read through {@link Host}, and user interactions are forwarded back to it. This
 * is the Phase-1 split that decouples the list UI from the Activity; in Phase 2 the
 * Host is backed by {@code MainViewModel} state.
 */
public class PostsAdapter extends BaseAdapter {

    /** Everything the adapter needs from its owner (Activity), backed by the ViewModel. */
    public interface Host {
        HNFeed getFeed();

        boolean isRead(HNPost post);

        boolean isLoadMoreInFlight();

        int getFontSizeTitle();

        int getFontSizeDetails();

        int getTitleColor();

        int getTitleReadColor();

        void onOpenComments(HNPost post);

        void onPostClicked(HNPost post);

        void onLongPress(HNPost post);

        void onLoadMore();
    }

    private static final int VIEWTYPE_POST = 0;
    private static final int VIEWTYPE_LOADMORE = 1;

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final Host mHost;

    public PostsAdapter(Context context, Host host) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mHost = host;
    }

    @Override
    public int getCount() {
        HNFeed feed = mHost.getFeed();
        if (feed == null || feed.getPosts() == null) {
            return 0;
        }
        int posts = feed.getPosts().size();
        if (posts == 0) {
            return 0;
        }
        return posts + (feed.isLoadedMore() ? 0 : 1);
    }

    @Override
    public HNPost getItem(int position) {
        if (getItemViewType(position) == VIEWTYPE_POST) {
            return mHost.getFeed().getPosts().get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return position < mHost.getFeed().getPosts().size() ? VIEWTYPE_POST : VIEWTYPE_LOADMORE;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
        case VIEWTYPE_POST:
            return getPostView(position, convertView);
        case VIEWTYPE_LOADMORE:
            return getLoadMoreView();
        default:
            return convertView;
        }
    }

    private View getPostView(int position, View convertView) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.main_list_item, null);
            PostViewHolder holder = new PostViewHolder();
            holder.titleView = (TextView) convertView.findViewById(R.id.main_list_item_title);
            holder.urlView = (TextView) convertView.findViewById(R.id.main_list_item_url);
            holder.textContainer = (android.widget.LinearLayout) convertView
                    .findViewById(R.id.main_list_item_textcontainer);
            holder.commentsButton = (android.widget.Button) convertView
                    .findViewById(R.id.main_list_item_comments_button);
            holder.commentsButton.setTypeface(FontHelper.getComfortaa(mContext, false));
            holder.pointsView = (TextView) convertView.findViewById(R.id.main_list_item_points);
            holder.pointsView.setTypeface(FontHelper.getComfortaa(mContext, true));
            convertView.setTag(holder);
        }

        final HNPost item = getItem(position);
        PostViewHolder holder = (PostViewHolder) convertView.getTag();

        holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mHost.getFontSizeTitle());
        holder.titleView.setText(item.getTitle());
        holder.titleView.setTextColor(mHost.isRead(item) ? mHost.getTitleReadColor() : mHost.getTitleColor());

        holder.urlView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mHost.getFontSizeDetails());
        holder.urlView.setText(item.getURLDomain());

        holder.pointsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mHost.getFontSizeDetails());
        if (item.getPoints() != BaseHTMLParser.UNDEFINED) {
            holder.pointsView.setText(item.getPoints() + "");
        } else {
            holder.pointsView.setText("-");
        }

        holder.commentsButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mHost.getFontSizeTitle());
        if (item.getCommentsCount() != BaseHTMLParser.UNDEFINED) {
            holder.commentsButton.setVisibility(View.VISIBLE);
            holder.commentsButton.setText(item.getCommentsCount() + "");
        } else {
            holder.commentsButton.setVisibility(View.INVISIBLE);
        }

        holder.commentsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mHost.onOpenComments(item);
            }
        });
        holder.textContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mHost.onPostClicked(item);
            }
        });
        holder.textContainer.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mHost.onLongPress(item);
                return true;
            }
        });

        return convertView;
    }

    private View getLoadMoreView() {
        // Single cell - intentionally not reusing a convertView.
        final View convertView = mInflater.inflate(R.layout.main_list_item_loadmore, null);
        final TextView textView = (TextView) convertView
                .findViewById(R.id.main_list_item_loadmore_text);
        textView.setTypeface(FontHelper.getComfortaa(mContext, true));
        final ImageView imageView = (ImageView) convertView
                .findViewById(R.id.main_list_item_loadmore_loadingimage);

        if (mHost.isLoadMoreInFlight()) {
            textView.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            convertView.setClickable(false);
        }

        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.VISIBLE);
                convertView.setClickable(false);
                mHost.onLoadMore();
            }
        });

        return convertView;
    }
}
