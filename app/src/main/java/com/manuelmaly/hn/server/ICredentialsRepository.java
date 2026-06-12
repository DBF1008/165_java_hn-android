package com.manuelmaly.hn.server;

import cz.msebera.android.httpclient.client.CookieStore;

/**
 * Holds the HN authentication cookie store. Injectable replacement for the static
 * {@link HNCredentials}; the static facade delegates here.
 */
public interface ICredentialsRepository {

    CookieStore getCookieStore();

    void invalidate();

    boolean isInvalidated();

}
