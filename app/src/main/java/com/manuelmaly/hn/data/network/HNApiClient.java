package com.manuelmaly.hn.data.network;

import java.util.Map;

import cz.msebera.android.httpclient.client.CookieStore;

/**
 * Abstraction over the HTTP command layer for testability.
 * Wraps StringDownloadCommand, GetHNUserTokenHTTPCommand, HNVoteCommand.
 */
public interface HNApiClient {

    /**
     * Downloads HTML from the given URL with optional query params and cookies.
     *
     * @return the HTML response body as a String
     * @throws Exception on network or HTTP errors
     */
    String downloadHtml(String url, Map<String, String> queryParams,
                        CookieStore cookieStore) throws Exception;

    /**
     * Performs login and returns the user token, or null on failure.
     */
    String loginAndGetToken(String username, String password) throws Exception;

    /**
     * Casts a vote at the given URL.
     *
     * @return true on success, false/null on failure
     */
    Boolean vote(String voteUrl, CookieStore cookieStore) throws Exception;
}
