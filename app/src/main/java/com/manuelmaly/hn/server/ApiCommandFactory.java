package com.manuelmaly.hn.server;

import cz.msebera.android.httpclient.client.CookieStore;

/**
 * Factory for the network commands the task layer needs. Tasks depend on this
 * interface instead of {@code new}-ing concrete {@link BaseHTTPCommand}
 * subclasses, so tests can substitute a fake factory that returns canned
 * commands.
 */
public interface ApiCommandFactory {

    /** GET the given feed URL (main feed / load-more), authenticated by the cookie store. */
    IAPICommand<String> createFeedDownload(String url, CookieStore cookieStore);

    /** GET the comments page for the given post id, authenticated by the cookie store. */
    IAPICommand<String> createCommentsDownload(String postId, CookieStore cookieStore);

    /** GET the vote URL for a post/comment, authenticated by the cookie store. */
    IAPICommand<Boolean> createVote(String voteUrl, CookieStore cookieStore);

    /** POST login credentials and obtain the user token (from the response cookie). */
    IAPICommand<String> createLoginToken(String username, String password);

}
