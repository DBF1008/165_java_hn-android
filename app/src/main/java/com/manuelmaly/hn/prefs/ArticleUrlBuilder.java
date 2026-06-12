package com.manuelmaly.hn.prefs;

import java.net.URLEncoder;

/**
 * Builds the URL loaded for an article from the chosen {@link HtmlProvider} and the post's raw URL.
 * This is the single home for the provider-to-URL mapping that previously lived as an
 * {@code if/else} chain in {@code ArticleReaderActivity.getArticleViewURL()}.
 *
 * <p>Pure (no Android dependencies). Behaviour is preserved exactly: the original article URL is
 * returned unencoded, while the reader/proxy services receive the URL-encoded address.
 */
public final class ArticleUrlBuilder {

    private static final String PREFIX_VIEWTEXT = "http://viewtext.org/article?url=";
    private static final String PREFIX_GOOGLE = "http://www.google.com/gwt/x?u=";
    private static final String PREFIX_INSTAPAPER = "http://www.instapaper.com/text?u=";

    private ArticleUrlBuilder() {
    }

    @SuppressWarnings("deprecation")
    public static String build(HtmlProvider provider, String rawUrl) {
        switch (provider) {
        case VIEWTEXT:
            return PREFIX_VIEWTEXT + URLEncoder.encode(rawUrl);
        case GOOGLE:
            return PREFIX_GOOGLE + URLEncoder.encode(rawUrl);
        case INSTAPAPER:
            return PREFIX_INSTAPAPER + URLEncoder.encode(rawUrl);
        case ORIGINAL_ARTICLE_URL:
        default:
            return rawUrl;
        }
    }
}
