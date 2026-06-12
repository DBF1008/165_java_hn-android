package com.manuelmaly.hn.prefs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Guards the font-size pixel sizes that were previously hardcoded and duplicated across
 * {@code MainActivity.refreshFontSizes()} and {@code CommentsActivity.refreshFontSizes()}.
 */
public class FontSizeProfileTest {

    @Test
    public void smallProfileHasExpectedSizes() {
        FontSizeProfile p = FontSizeProfile.of(FontSize.SMALL);
        assertEquals(15, p.listTitle());
        assertEquals(11, p.listDetails());
        assertEquals(14, p.commentText());
        assertEquals(12, p.commentMetadata());
    }

    @Test
    public void normalProfileHasExpectedSizes() {
        FontSizeProfile p = FontSizeProfile.of(FontSize.NORMAL);
        assertEquals(18, p.listTitle());
        assertEquals(12, p.listDetails());
        assertEquals(16, p.commentText());
        assertEquals(14, p.commentMetadata());
    }

    @Test
    public void bigProfileHasExpectedSizes() {
        FontSizeProfile p = FontSizeProfile.of(FontSize.BIG);
        assertEquals(22, p.listTitle());
        assertEquals(15, p.listDetails());
        assertEquals(20, p.commentText());
        assertEquals(18, p.commentMetadata());
    }
}
