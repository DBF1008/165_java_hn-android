package com.manuelmaly.hn.viewmodel;

import com.manuelmaly.hn.model.HNComment;

/**
 * UI-ready model for a comment item in the comments list.
 * Combines the comment data with display parameters so that
 * the adapter doesn't need to compute them during binding.
 */
public class CommentUiModel {

    public final HNComment comment;
    public final int indentLevel;
    public final boolean isExpanded;
    public final boolean hasChildren;

    public CommentUiModel(HNComment comment, int indentLevel,
                          boolean isExpanded, boolean hasChildren) {
        this.comment = comment;
        this.indentLevel = indentLevel;
        this.isExpanded = isExpanded;
        this.hasChildren = hasChildren;
    }
}
