package com.manuelmaly.hn.storage;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.Settings;
import com.manuelmaly.hn.parser.IUserContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Default {@link ISettingsRepository} (and {@link IUserContext}). Reproduces the
 * logic of the former static {@link Settings}, bound to the application context.
 * Shares the same {@code PREF_*} keys / default string resources, so values read
 * and written here are identical to the legacy code.
 */
@Singleton
public class SettingsRepositoryImpl implements ISettingsRepository, IUserContext {

    private final Application mApp;

    @Inject
    public SettingsRepositoryImpl(Application app) {
        mApp = app;
    }

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(mApp);
    }

    @Override
    public String getFontSize() {
        return prefs().getString(Settings.PREF_FONTSIZE, mApp.getString(R.string.pref_default_fontsize));
    }

    @Override
    public String getHtmlProvider() {
        return prefs().getString(Settings.PREF_HTMLPROVIDER, mApp.getString(R.string.pref_default_htmlprovider));
    }

    @Override
    public String getHtmlViewer() {
        return prefs().getString(Settings.PREF_HTMLVIEWER, mApp.getString(R.string.pref_default_htmlviewer));
    }

    @Override
    public boolean isPullDownRefresh() {
        return prefs().getBoolean(Settings.PREF_PULLDOWNREFRESH, false);
    }

    @Override
    public boolean isUserLoggedIn() {
        String userName = getUserName();
        return userName != null && !userName.isEmpty();
    }

    @Override
    public String getUserName() {
        String[] userData = prefs().getString(Settings.PREF_USER, "").split(Settings.USER_DATA_SEPARATOR);
        if (userData.length > 0)
            return userData[0];
        return null;
    }

    @Override
    public String getUserToken() {
        String[] userData = prefs().getString(Settings.PREF_USER, "").split(Settings.USER_DATA_SEPARATOR);
        if (userData.length > 1)
            return userData[1];
        return null;
    }

    @Override
    public void setUserData(String userName, String userToken) {
        prefs().edit().putString(Settings.PREF_USER, userName + Settings.USER_DATA_SEPARATOR + userToken).commit();
    }

    @Override
    public void clearUserData() {
        prefs().edit().remove(Settings.PREF_USER).commit();
    }

    @Override
    public String getCurrentUsername() {
        return getUserName();
    }

}
