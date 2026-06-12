package com.manuelmaly.hn.parser;

import com.manuelmaly.hn.data.storage.AppSettings;
import com.manuelmaly.hn.model.HNPostComments;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Adapter that delegates to HNCommentsParser, injecting the current user from AppSettings.
 */
@Singleton
public class HNCommentsParserAdapter implements CommentsParser {

    private final AppSettings appSettings;

    @Inject
    public HNCommentsParserAdapter(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    @Override
    public HNPostComments parse(String html) throws Exception {
        String currentUser = appSettings.getUserName();
        return new HNCommentsParser(currentUser).parse(html);
    }
}
