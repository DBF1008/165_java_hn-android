package com.manuelmaly.hn.data;

import com.manuelmaly.hn.App;
import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPostComments;
import com.manuelmaly.hn.parser.HNCommentsParser;
import com.manuelmaly.hn.parser.HNFeedParser;
import com.manuelmaly.hn.server.HNCredentials;
import com.manuelmaly.hn.server.HNVoteCommand;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.server.StringDownloadCommand;

import cz.msebera.android.httpclient.client.CookieStore;

/**
 * Production {@link IHNRemoteDataSource}. Each call constructs the same HTTP
 * command + HTML parser the old task framework used, runs it synchronously, and
 * maps the command's {@link IAPICommand} error code onto a result or a
 * {@link HNApiException}. Holds no Activity/Context state (uses
 * {@link App#getInstance()} for application context, like the parsers do).
 */
public class HNRemoteDataSource implements IHNRemoteDataSource {

    private static final String FEED_URL = "https://news.ycombinator.com/";
    private static final String COMMENTS_URL_PREFIX = "https://news.ycombinator.com/item?id=";

    @Override
    public HNFeed fetchFeed() throws HNApiException {
        return parseFeed(download(FEED_URL));
    }

    @Override
    public HNFeed fetchMore(HNFeed previous) throws HNApiException {
        String nextPageUrl = previous != null ? previous.getNextPageURL() : null;
        if (nextPageUrl == null) {
            // Nothing more to load - treat as an error so the caller can stop paging.
            throw new HNApiException(IAPICommand.ERROR_UNKNOWN);
        }
        return parseFeed(download(nextPageUrl));
    }

    @Override
    public HNPostComments fetchComments(String postId) throws HNApiException {
        String html = download(COMMENTS_URL_PREFIX + postId);
        try {
            return new HNCommentsParser().parse(html);
        } catch (Exception e) {
            throw new HNApiException(IAPICommand.ERROR_RESPONSE_PARSE_ERROR);
        }
    }

    @Override
    public boolean vote(String voteUrl) throws HNApiException {
        HNVoteCommand command = new HNVoteCommand(voteUrl, null, IAPICommand.RequestType.GET,
                false, null, App.getInstance(), cookieStore());
        command.run();
        if (command.getErrorCode() != IAPICommand.ERROR_NONE) {
            // Communication failure (offline, timeout, server error). Unlike the old
            // HNVoteTask, we surface this instead of swallowing it.
            throw new HNApiException(command.getErrorCode());
        }
        // A completed request whose body failed validation (e.g. "must be logged in")
        // returns Boolean.FALSE with ERROR_NONE.
        return Boolean.TRUE.equals(command.getResponseContent());
    }

    private HNFeed parseFeed(String html) throws HNApiException {
        try {
            return new HNFeedParser().parse(html);
        } catch (Exception e) {
            throw new HNApiException(IAPICommand.ERROR_RESPONSE_PARSE_ERROR);
        }
    }

    private String download(String url) throws HNApiException {
        StringDownloadCommand command = new StringDownloadCommand(url, null,
                IAPICommand.RequestType.GET, false, null, App.getInstance(), cookieStore());
        command.run();
        if (command.getErrorCode() != IAPICommand.ERROR_NONE) {
            throw new HNApiException(command.getErrorCode());
        }
        return command.getResponseContent();
    }

    private CookieStore cookieStore() {
        return HNCredentials.getCookieStore(App.getInstance());
    }
}
