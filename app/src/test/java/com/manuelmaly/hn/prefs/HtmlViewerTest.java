package com.manuelmaly.hn.prefs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HtmlViewerTest {

    @Test
    public void browserOpensInBrowser() {
        assertTrue(HtmlViewer.BROWSER.opensInBrowser());
    }

    @Test
    public void appDoesNotOpenInBrowser() {
        assertFalse(HtmlViewer.APP.opensInBrowser());
    }
}
