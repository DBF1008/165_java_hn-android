package com.manuelmaly.hn.parser;

import com.manuelmaly.hn.model.HNPostComments;

/**
 * Abstraction over HTML comments parsing for testability.
 * Wraps HNCommentsParser.
 */
public interface CommentsParser {

    HNPostComments parse(String html) throws Exception;
}
