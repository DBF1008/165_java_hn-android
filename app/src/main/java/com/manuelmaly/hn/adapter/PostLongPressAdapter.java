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
import com.manuelmaly.hn.model.HNPost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Adapter for the long-press context menu on a post item.
 * Extracted from MainActivity.LongPressMenuListAdapter.
 *
 * Implements both {@link ListAdapter} (for AlertDialog) and
 * {@link android.content.DialogInterface.OnClickListener} (for item selection).
 */
public class PostLongPressAdapter implements ListAdapter, DialogInterface.OnClickListener {

    private final HNPost post;
    private final boolean isLoggedIn;
    private final boolean upVotingEnabled;
    private final ArrayList<CharSequence> items;
    private final Context context;
    private final LayoutInflater inflater;
    private final Callbacks callbacks;

    /**
     * Callback interface for menu item actions.
     */
    public interface Callbacks {
        void onUpvote(HNPost post);
        void onOpenInApp(HNPost post, String htmlProvider);
        void onOpenInBrowser(HNPost post);
        void onShare(HNPost post);
    }

    public PostLongPressAdapter(Context context, LayoutInflater inflater,
                                HNPost post, Set<HNPost> upvotedPosts,
                                Callbacks callbacks) {
        this.context = context;
        this.inflater = inflater;
        this.post = post;
        this.callbacks = callbacks;

        isLoggedIn = Settings.isUserLoggedIn(context);
        String userName = Settings.getUserName(context);
        upVotingEnabled = !isLoggedIn
                || (post.getUpvoteURL(userName) != null && !upvotedPosts.contains(post));

        items = new ArrayList<CharSequence>();
        if (upVotingEnabled) {
            items.add(context.getString(R.string.upvote));
        } else {
            items.add(context.getString(R.string.already_upvoted));
        }
        items.addAll(Arrays.asList(
                context.getString(R.string.pref_htmlprovider_original_url),
                context.getString(R.string.pref_htmlprovider_viewtext),
                context.getString(R.string.pref_htmlprovider_google),
                context.getString(R.string.pref_htmlprovider_instapaper),
                context.getString(R.string.external_browser),
                context.getString(R.string.share_article_url)));
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
        switch (item) {
            case 0:
                if (!isLoggedIn) {
                    android.widget.Toast.makeText(context, R.string.please_log_in,
                            android.widget.Toast.LENGTH_LONG).show();
                } else if (upVotingEnabled) {
                    callbacks.onUpvote(post);
                }
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                callbacks.onOpenInApp(post, getItem(item).toString());
                break;
            case 5:
                callbacks.onOpenInBrowser(post);
                break;
            case 6:
                callbacks.onShare(post);
                break;
        }
    }
}
