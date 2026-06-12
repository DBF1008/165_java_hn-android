package com.manuelmaly.hn.prefs;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Covers the <b>settings-change</b> scenario: when the stored preference value changes, the resolver
 * yields the corresponding enum. Also pins the fallbacks for unrecognized/null values, which mirror
 * the original per-Activity {@code else} branches (font &rarr; BIG, provider &rarr; original,
 * viewer &rarr; app).
 *
 * <p>The label strings below are exactly those defined in
 * {@code app/src/main/res/values/preference_values.xml}.
 */
public class PreferenceValueResolverTest {

    private PreferenceValueResolver resolver;

    @Before
    public void setUp() {
        resolver = new PreferenceValueResolver(
            "Small", "Normal", "Big",
            "Original Article URL", "ViewText.org", "Google for Mobile Devices", "Instapaper Text",
            "The App", "The System Browser");
    }

    @Test
    public void resolvesKnownFontSizes() {
        assertEquals(FontSize.SMALL, resolver.fontSize("Small"));
        assertEquals(FontSize.NORMAL, resolver.fontSize("Normal"));
        assertEquals(FontSize.BIG, resolver.fontSize("Big"));
    }

    @Test
    public void unknownFontSizeFallsBackToBig() {
        assertEquals(FontSize.BIG, resolver.fontSize("Gigantic"));
        assertEquals(FontSize.BIG, resolver.fontSize(null));
    }

    @Test
    public void resolvesKnownProviders() {
        assertEquals(HtmlProvider.ORIGINAL_ARTICLE_URL, resolver.htmlProvider("Original Article URL"));
        assertEquals(HtmlProvider.VIEWTEXT, resolver.htmlProvider("ViewText.org"));
        assertEquals(HtmlProvider.GOOGLE, resolver.htmlProvider("Google for Mobile Devices"));
        assertEquals(HtmlProvider.INSTAPAPER, resolver.htmlProvider("Instapaper Text"));
    }

    @Test
    public void unknownProviderFallsBackToOriginal() {
        assertEquals(HtmlProvider.ORIGINAL_ARTICLE_URL, resolver.htmlProvider("Pocket"));
        assertEquals(HtmlProvider.ORIGINAL_ARTICLE_URL, resolver.htmlProvider(null));
    }

    @Test
    public void resolvesKnownViewers() {
        assertEquals(HtmlViewer.APP, resolver.htmlViewer("The App"));
        assertEquals(HtmlViewer.BROWSER, resolver.htmlViewer("The System Browser"));
    }

    @Test
    public void unknownViewerFallsBackToApp() {
        assertEquals(HtmlViewer.APP, resolver.htmlViewer("Firefox"));
        assertEquals(HtmlViewer.APP, resolver.htmlViewer(null));
    }
}
