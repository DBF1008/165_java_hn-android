package com.manuelmaly.hn.server;

import android.content.Context;

import com.manuelmaly.hn.App;

import cz.msebera.android.httpclient.client.CookieStore;

/**
 * Static facade over the HN authentication cookie store, kept for source
 * compatibility. Delegates to the injectable, singleton
 * {@link ICredentialsRepository} (which holds the shared cookie cache and
 * invalidation state), obtained from the Dagger component.
 */
public class HNCredentials {

    public static CookieStore getCookieStore(Context c) {
        return App.component().credentialsRepository().getCookieStore();
    }

    public static void invalidate() {
        App.component().credentialsRepository().invalidate();
    }

    public static boolean isInvalidated() {
        return App.component().credentialsRepository().isInvalidated();
    }

}
