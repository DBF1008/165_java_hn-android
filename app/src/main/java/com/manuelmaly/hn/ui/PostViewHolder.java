package com.manuelmaly.hn.ui;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Row view holder for the feed list. Extracted from {@code MainActivity} so the
 * post-row view lookups live with the adapter rather than in the Activity.
 */
public class PostViewHolder {
    public TextView titleView;
    public TextView urlView;
    public TextView pointsView;
    public LinearLayout textContainer;
    public Button commentsButton;
}
