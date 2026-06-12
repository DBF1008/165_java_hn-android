package com.manuelmaly.hn.parser;

import com.manuelmaly.hn.model.HNPostComments;

/**
 * Parses an HN item/comments HTML document into {@link HNPostComments}.
 * Implemented by {@link HNCommentsParser}; injected into the comments task.
 */
public interface ICommentsParser {

    HNPostComments parse(String html) throws Exception;

}
