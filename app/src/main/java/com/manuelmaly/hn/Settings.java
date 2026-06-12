package com.manuelmaly.hn;

import android.content.Context;

/**
 * Static facade over user settings, kept for source compatibility with the many
 * existing call sites. The {@code Context} parameters are now vestigial — all calls
 * delegate to the injectable {@code ISettingsRepository} (bound to the application
 * context, which is what {@code getDefaultSharedPreferences} used anyway), obtained
 * from the Dagger component.
 */
public class Settings {

    public static final String PREF_FONTSIZE = "pref_fontsize";
    public static final String PREF_HTMLPROVIDER = "pref_htmlprovider";
    public static final String PREF_HTMLVIEWER = "pref_htmlviewer";
    public static final String PREF_USER = "pref_user";
    public static final String PREF_REPORTING = "pref_crashlytics";
    public static final String PREF_PULLDOWNREFRESH = "pref_pulldownrefresh";

    public static final String USER_DATA_SEPARATOR = ":";

    public static String getFontSize(Context c) {
        return App.component().settingsRepository().getFontSize();
    }

    public static String getHtmlProvider(Context c) {
        return App.component().settingsRepository().getHtmlProvider();
    }

    public static String getHtmlViewer(Context c) {
        return App.component().settingsRepository().getHtmlViewer();
    }

    public static boolean isPullDownRefresh(Context c) {
        return App.component().settingsRepository().isPullDownRefresh();
    }

    public static boolean isUserLoggedIn(Context c) {
        return App.component().settingsRepository().isUserLoggedIn();
    }

    public static String getUserName(Context c) {
        return App.component().settingsRepository().getUserName();
    }

    public static String getUserToken(Context c) {
        return App.component().settingsRepository().getUserToken();
    }

    public static void setUserData(String userName, String userToken, Context c) {
        App.component().settingsRepository().setUserData(userName, userToken);
    }

    public static void clearUserData(Context c) {
        App.component().settingsRepository().clearUserData();
    }

}
