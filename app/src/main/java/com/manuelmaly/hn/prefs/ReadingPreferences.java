package com.manuelmaly.hn.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.Settings;

/**
 * Single entry point ("settings repository") for the reading preferences — font size, HTML provider,
 * HTML viewer and pull-to-refresh. It reads the persisted {@link SharedPreferences} values and turns
 * them into the typed enums of this package via {@link PreferenceValueResolver}, so the list,
 * comments and reading screens no longer each parse raw preference strings.
 *
 * <p>The stored-value keys and the per-key missing-value defaults match what {@code Settings}
 * previously used, so behaviour is preserved.
 */
public class ReadingPreferences {

    private final SharedPreferences sharedPreferences;
    private final PreferenceValueResolver resolver;
    private final String fontSizeDefault;
    private final String htmlProviderDefault;
    private final String htmlViewerDefault;

    public ReadingPreferences(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        resolver = new PreferenceValueResolver(
            context.getString(R.string.pref_fontsize_small),
            context.getString(R.string.pref_fontsize_normal),
            context.getString(R.string.pref_fontsize_big),
            context.getString(R.string.pref_htmlprovider_original_url),
            context.getString(R.string.pref_htmlprovider_viewtext),
            context.getString(R.string.pref_htmlprovider_google),
            context.getString(R.string.pref_htmlprovider_instapaper),
            context.getString(R.string.pref_htmlviewer_app),
            context.getString(R.string.pref_htmlviewer_browser));
        fontSizeDefault = context.getString(R.string.pref_default_fontsize);
        htmlProviderDefault = context.getString(R.string.pref_default_htmlprovider);
        htmlViewerDefault = context.getString(R.string.pref_default_htmlviewer);
    }

    public FontSize getFontSize() {
        return resolver.fontSize(
            sharedPreferences.getString(Settings.PREF_FONTSIZE, fontSizeDefault));
    }

    public HtmlProvider getHtmlProvider() {
        return resolver.htmlProvider(
            sharedPreferences.getString(Settings.PREF_HTMLPROVIDER, htmlProviderDefault));
    }

    public HtmlViewer getHtmlViewer() {
        return resolver.htmlViewer(
            sharedPreferences.getString(Settings.PREF_HTMLVIEWER, htmlViewerDefault));
    }

    public boolean isPullDownRefresh() {
        return sharedPreferences.getBoolean(Settings.PREF_PULLDOWNREFRESH, false);
    }

    /**
     * Resolves the provider for a single article load: the per-launch override (a provider display
     * string passed via Intent, e.g. from the list's long-press menu) when present, otherwise the
     * stored preference.
     */
    public HtmlProvider resolveHtmlProvider(String overrideDisplayValue) {
        if (overrideDisplayValue != null) {
            return resolver.htmlProvider(overrideDisplayValue);
        }
        return getHtmlProvider();
    }

    /**
     * Builds the URL for an article, applying provider resolution (override or stored). Pass a {@code
     * null} override to use the stored provider.
     */
    public String articleUrl(String rawUrl, String overrideDisplayValue) {
        return ArticleUrlBuilder.build(resolveHtmlProvider(overrideDisplayValue), rawUrl);
    }
}
