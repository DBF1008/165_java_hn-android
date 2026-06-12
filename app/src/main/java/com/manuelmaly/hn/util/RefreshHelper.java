package com.manuelmaly.hn.util;

import android.view.LayoutInflater;
import android.view.View;

import androidx.core.view.MenuItemCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.Menu;
import android.view.MenuItem;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.Settings;

/**
 * Manages the refresh indicator state across both SwipeRefreshLayout and
 * the action bar refresh icon. Extracted from the duplicate setShowRefreshing()
 * logic previously in MainActivity and CommentsActivity.
 */
public class RefreshHelper {

    private final SwipeRefreshLayout swipeRefreshLayout;
    private final LayoutInflater inflater;
    private final MenuCallback menuCallback;
    private boolean shouldShowRefreshing = false;

    /**
     * Callback to trigger action bar menu invalidation.
     */
    public interface MenuCallback {
        void invalidateOptionsMenu();
    }

    public RefreshHelper(SwipeRefreshLayout swipeRefreshLayout,
                         LayoutInflater inflater,
                         MenuCallback menuCallback) {
        this.swipeRefreshLayout = swipeRefreshLayout;
        this.inflater = inflater;
        this.menuCallback = menuCallback;
    }

    /**
     * Updates the refresh indicator state. Handles both the SwipeRefreshLayout
     * spinner and the action bar refresh icon animation.
     *
     * @param showRefreshing true to show the refreshing indicator, false to hide it
     * @param context        Context for reading Settings preferences
     */
    public void setShowRefreshing(boolean showRefreshing, android.content.Context context) {
        if (!Settings.isPullDownRefresh(context)) {
            shouldShowRefreshing = showRefreshing;
            if (menuCallback != null) {
                menuCallback.invalidateOptionsMenu();
            }
        }

        if (swipeRefreshLayout.isEnabled()
                && (!swipeRefreshLayout.isRefreshing() || !showRefreshing)) {
            swipeRefreshLayout.setRefreshing(showRefreshing);
        }
    }

    /**
     * Call from onPrepareOptionsMenu to update the refresh icon state.
     */
    public void prepareRefreshMenuItem(Menu menu, MenuItem refreshItem) {
        if (!shouldShowRefreshing) {
            MenuItemCompat.setActionView(refreshItem, null);
        } else {
            View v = inflater.inflate(R.layout.refresh_icon, null);
            MenuItemCompat.setActionView(refreshItem, v);
        }
    }

    public boolean isShouldShowRefreshing() {
        return shouldShowRefreshing;
    }
}
