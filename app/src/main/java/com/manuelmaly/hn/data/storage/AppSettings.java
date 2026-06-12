package com.manuelmaly.hn.data.storage;

/**
 * Abstraction over SharedPreferences-based settings for testability.
 * Wraps the static Settings utility class.
 */
public interface AppSettings {

    String getFontSize();

    String getHtmlProvider();

    String getHtmlViewer();

    boolean isPullDownRefresh();

    boolean isUserLoggedIn();

    String getUserName();

    String getUserToken();

    void setUserData(String userName, String userToken);

    void clearUserData();
}
