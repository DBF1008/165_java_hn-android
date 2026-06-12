package com.manuelmaly.hn.parser;

import com.manuelmaly.hn.data.storage.AppSettings;
import com.manuelmaly.hn.model.HNFeed;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Adapter that delegates to HNFeedParser, injecting the current user from AppSettings.
 */
@Singleton
public class HNFeedParserAdapter implements FeedParser {

    private final AppSettings appSettings;

    @Inject
    public HNFeedParserAdapter(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    @Override
    public HNFeed parse(String html) throws Exception {
        String currentUser = appSettings.getUserName();
        return new HNFeedParser(currentUser).parse(html);
    }
}
