package com.manuelmaly.hn.prefs;

/**
 * The source used to render an article's HTML (original page, or a reader/proxy service).
 *
 * <p>Pure (no Android dependencies); {@link ArticleUrlBuilder} turns a provider into a URL.
 */
public enum HtmlProvider {
    ORIGINAL_ARTICLE_URL, VIEWTEXT, GOOGLE, INSTAPAPER
}
