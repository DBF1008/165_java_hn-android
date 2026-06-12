package com.manuelmaly.hn.prefs;

/**
 * Maps stored preference values (the label strings persisted by the settings UI) to typed enums.
 * It is constructed with the resolved label strings — supplied by {@link ReadingPreferences} from
 * Android resources — so it stays free of Android dependencies and can be unit-tested directly.
 *
 * <p>Fallbacks for an unrecognized value mirror the original per-Activity {@code else} branches, so
 * the refactor is behaviour-preserving even for unexpected stored values:
 * <ul>
 *   <li>font size &rarr; {@link FontSize#BIG} (the final {@code else} in {@code refreshFontSizes()})</li>
 *   <li>provider &rarr; {@link HtmlProvider#ORIGINAL_ARTICLE_URL} (the {@code else} in {@code getArticleViewURL()})</li>
 *   <li>viewer &rarr; {@link HtmlViewer#APP} (anything that is not the browser value)</li>
 * </ul>
 */
public final class PreferenceValueResolver {

    private final String fontSizeSmall;
    private final String fontSizeNormal;
    private final String fontSizeBig;

    private final String providerOriginal;
    private final String providerViewtext;
    private final String providerGoogle;
    private final String providerInstapaper;

    private final String viewerApp;
    private final String viewerBrowser;

    public PreferenceValueResolver(String fontSizeSmall, String fontSizeNormal, String fontSizeBig,
        String providerOriginal, String providerViewtext, String providerGoogle,
        String providerInstapaper, String viewerApp, String viewerBrowser) {
        this.fontSizeSmall = fontSizeSmall;
        this.fontSizeNormal = fontSizeNormal;
        this.fontSizeBig = fontSizeBig;
        this.providerOriginal = providerOriginal;
        this.providerViewtext = providerViewtext;
        this.providerGoogle = providerGoogle;
        this.providerInstapaper = providerInstapaper;
        this.viewerApp = viewerApp;
        this.viewerBrowser = viewerBrowser;
    }

    public FontSize fontSize(String stored) {
        if (fontSizeSmall.equals(stored)) {
            return FontSize.SMALL;
        }
        if (fontSizeNormal.equals(stored)) {
            return FontSize.NORMAL;
        }
        if (fontSizeBig.equals(stored)) {
            return FontSize.BIG;
        }
        return FontSize.BIG;
    }

    public HtmlProvider htmlProvider(String stored) {
        if (providerViewtext.equals(stored)) {
            return HtmlProvider.VIEWTEXT;
        }
        if (providerGoogle.equals(stored)) {
            return HtmlProvider.GOOGLE;
        }
        if (providerInstapaper.equals(stored)) {
            return HtmlProvider.INSTAPAPER;
        }
        if (providerOriginal.equals(stored)) {
            return HtmlProvider.ORIGINAL_ARTICLE_URL;
        }
        return HtmlProvider.ORIGINAL_ARTICLE_URL;
    }

    public HtmlViewer htmlViewer(String stored) {
        if (viewerBrowser.equals(stored)) {
            return HtmlViewer.BROWSER;
        }
        if (viewerApp.equals(stored)) {
            return HtmlViewer.APP;
        }
        return HtmlViewer.APP;
    }
}
