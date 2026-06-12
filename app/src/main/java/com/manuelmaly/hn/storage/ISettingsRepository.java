package com.manuelmaly.hn.storage;

/**
 * Injectable view over user settings / preferences. Mirrors the static
 * {@link com.manuelmaly.hn.Settings} API but without the {@code Context} parameter
 * (the implementation is bound to the application context). The static facade
 * delegates here.
 */
public interface ISettingsRepository {

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
