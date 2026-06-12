package com.manuelmaly.hn.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.net.URLEncoder;

import org.junit.Test;

/**
 * Guards the provider-to-URL mapping extracted from {@code ArticleReaderActivity.getArticleViewURL()}:
 * the original article URL is returned unencoded, the reader/proxy services receive the encoded URL.
 */
public class ArticleUrlBuilderTest {

    private static final String RAW = "http://example.com/a b?x=1&y=2";

    @SuppressWarnings("deprecation")
    private static String encoded() {
        return URLEncoder.encode(RAW);
    }

    @Test
    public void originalReturnsRawUrlUnencoded() {
        assertEquals(RAW, ArticleUrlBuilder.build(HtmlProvider.ORIGINAL_ARTICLE_URL, RAW));
    }

    @Test
    public void viewtextWrapsEncodedUrl() {
        assertEquals("http://viewtext.org/article?url=" + encoded(),
            ArticleUrlBuilder.build(HtmlProvider.VIEWTEXT, RAW));
    }

    @Test
    public void googleWrapsEncodedUrl() {
        assertEquals("http://www.google.com/gwt/x?u=" + encoded(),
            ArticleUrlBuilder.build(HtmlProvider.GOOGLE, RAW));
    }

    @Test
    public void instapaperWrapsEncodedUrl() {
        assertEquals("http://www.instapaper.com/text?u=" + encoded(),
            ArticleUrlBuilder.build(HtmlProvider.INSTAPAPER, RAW));
    }

    @Test
    public void proxyProvidersEncodeRatherThanPassRawUrl() {
        // The encoded URL must differ from the raw one, i.e. encoding actually happened.
        assertNotEquals("http://viewtext.org/article?url=" + RAW,
            ArticleUrlBuilder.build(HtmlProvider.VIEWTEXT, RAW));
    }
}
