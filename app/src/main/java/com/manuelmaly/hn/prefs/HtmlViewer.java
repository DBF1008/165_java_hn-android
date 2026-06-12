package com.manuelmaly.hn.prefs;

/**
 * Where an article opens: inside the app's WebView, or the system browser.
 *
 * <p>Pure (no Android dependencies).
 */
public enum HtmlViewer {
    APP, BROWSER;

    public boolean opensInBrowser() {
        return this == BROWSER;
    }
}
