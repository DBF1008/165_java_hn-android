package com.manuelmaly.hn.server;

import com.manuelmaly.hn.storage.ISettingsRepository;

import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.impl.client.BasicCookieStore;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Default {@link ICredentialsRepository}. Holds the process-wide cookie store and
 * rebuilds it from the persisted user token — identical logic to the former static
 * {@link HNCredentials}. Must be a singleton so the cache / invalidation is shared.
 */
@Singleton
public class HNCredentialsRepositoryImpl implements ICredentialsRepository {

    private static final String COOKIE_USER = "user";

    private final ISettingsRepository mSettings;

    private CookieStore mCookieStore;
    private boolean mInvalidated;

    @Inject
    public HNCredentialsRepositoryImpl(ISettingsRepository settings) {
        mSettings = settings;
    }

    @Override
    public synchronized CookieStore getCookieStore() {
        if (mCookieStore != null && !mInvalidated)
            return mCookieStore;

        mCookieStore = new BasicCookieStore();
        String userToken = mSettings.getUserToken();

        if (userToken != null) {
            BasicClientCookie cookie = new BasicClientCookie(COOKIE_USER, userToken);
            cookie.setDomain("news.ycombinator.com");
            cookie.setPath("/");
            mCookieStore.addCookie(cookie);
        }

        mInvalidated = false;

        return mCookieStore;
    }

    @Override
    public void invalidate() {
        mCookieStore = null;
        mInvalidated = true;
    }

    @Override
    public boolean isInvalidated() {
        return mInvalidated;
    }

}
