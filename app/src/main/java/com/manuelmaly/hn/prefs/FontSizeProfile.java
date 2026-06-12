package com.manuelmaly.hn.prefs;

/**
 * Resolved text sizes (in dip) for a given {@link FontSize}, shared by the list and comments
 * screens. This is the "display configuration" for font size: it gathers the pixel sizes that were
 * previously hardcoded and duplicated in each Activity's {@code refreshFontSizes()} into one place.
 *
 * <p>Pure (no Android dependencies) so the mapping can be unit-tested directly.
 */
public final class FontSizeProfile {

    private final int listTitle;
    private final int listDetails;
    private final int commentText;
    private final int commentMetadata;

    private FontSizeProfile(int listTitle, int listDetails, int commentText, int commentMetadata) {
        this.listTitle = listTitle;
        this.listDetails = listDetails;
        this.commentText = commentText;
        this.commentMetadata = commentMetadata;
    }

    public static FontSizeProfile of(FontSize size) {
        switch (size) {
        case SMALL:
            return new FontSizeProfile(15, 11, 14, 12);
        case BIG:
            return new FontSizeProfile(22, 15, 20, 18);
        case NORMAL:
        default:
            return new FontSizeProfile(18, 12, 16, 14);
        }
    }

    /** Post title size on the list screen. */
    public int listTitle() {
        return listTitle;
    }

    /** URL/points/comment-count size on the list screen. */
    public int listDetails() {
        return listDetails;
    }

    /** Comment body size on the comments screen. */
    public int commentText() {
        return commentText;
    }

    /** Author/time-ago size on the comments screen. */
    public int commentMetadata() {
        return commentMetadata;
    }
}
