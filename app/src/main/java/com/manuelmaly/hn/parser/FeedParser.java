package com.manuelmaly.hn.parser;

import com.manuelmaly.hn.model.HNFeed;

/**
 * Abstraction over HTML feed parsing for testability.
 * Wraps HNFeedParser.
 */
public interface FeedParser {

    HNFeed parse(String html) throws Exception;
}
