package com.manuelmaly.hn.parser;

import com.manuelmaly.hn.model.HNFeed;

/**
 * Parses an HN front-page / feed HTML document into an {@link HNFeed}.
 * Implemented by {@link HNFeedParser}; injected into the feed tasks.
 */
public interface IFeedParser {

    HNFeed parse(String html) throws Exception;

}
