package com.manuelmaly.hn.adapter;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.model.HNPost;

/**
 * ViewHolder for a single post item in the main feed list.
 * Extracted from MainActivity.PostViewHolder.
 */
public class PostViewHolder {
    public TextView titleView;
    public TextView urlView;
    public TextView pointsView;
    public LinearLayout textContainer;
    public Button commentsButton;

    /**
     * The post bound to this holder. Updated on each getView() call
     * so that click listeners can always reference the current item.
     */
    HNPost currentPost;

    public PostViewHolder(View convertView) {
        titleView = (TextView) convertView.findViewById(R.id.main_list_item_title);
        urlView = (TextView) convertView.findViewById(R.id.main_list_item_url);
        textContainer = (LinearLayout) convertView.findViewById(R.id.main_list_item_textcontainer);
        commentsButton = (Button) convertView.findViewById(R.id.main_list_item_comments_button);
        pointsView = (TextView) convertView.findViewById(R.id.main_list_item_points);
    }
}
