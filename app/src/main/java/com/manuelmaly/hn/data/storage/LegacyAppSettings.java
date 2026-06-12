package com.manuelmaly.hn.data.storage;

import android.content.Context;

import com.manuelmaly.hn.Settings;

import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Legacy implementation that delegates to the static Settings utility.
 */
@Singleton
public class LegacyAppSettings implements AppSettings {

    private final Context context;

    @Inject
    public LegacyAppSettings(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override
    public String getFontSize() {
        return Settings.getFontSize(context);
    }

    @Override
    public String getHtmlProvider() {
        return Settings.getHtmlProvider(context);
    }

    @Override
    public String getHtmlViewer() {
        return Settings.getHtmlViewer(context);
    }

    @Override
    public boolean isPullDownRefresh() {
        return Settings.isPullDownRefresh(context);
    }

    @Override
    public boolean isUserLoggedIn() {
        return Settings.isUserLoggedIn(context);
    }

    @Override
    public String getUserName() {
        return Settings.getUserName(context);
    }

    @Override
    public String getUserToken() {
        return Settings.getUserToken(context);
    }

    @Override
    public void setUserData(String userName, String userToken) {
        Settings.setUserData(userName, userToken, context);
    }

    @Override
    public void clearUserData() {
        Settings.clearUserData(context);
    }
}
