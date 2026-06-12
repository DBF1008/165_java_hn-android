package com.manuelmaly.hn.data.network;

import android.content.Context;

import com.manuelmaly.hn.server.GetHNUserTokenHTTPCommand;
import com.manuelmaly.hn.server.HNVoteCommand;
import com.manuelmaly.hn.server.IAPICommand;
import com.manuelmaly.hn.server.IAPICommand.RequestType;
import com.manuelmaly.hn.server.StringDownloadCommand;

import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.client.CookieStore;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Legacy implementation that delegates to existing HTTP command classes.
 */
@Singleton
public class LegacyApiClient implements HNApiClient {

    private final Context context;

    @Inject
    public LegacyApiClient(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override
    public String downloadHtml(String url, Map<String, String> queryParams,
                               CookieStore cookieStore) throws Exception {
        HashMap<String, String> params = queryParams != null
                ? new HashMap<>(queryParams) : new HashMap<>();

        StringDownloadCommand command = new StringDownloadCommand(
                url, params, RequestType.GET, false, null, context, cookieStore);
        command.run();

        if (command.getErrorCode() != IAPICommand.ERROR_NONE) {
            throw new Exception("HTTP error code: " + command.getErrorCode());
        }

        return command.getResponseContent();
    }

    @Override
    public String loginAndGetToken(String username, String password) throws Exception {
        String url = "https://news.ycombinator.com/login";

        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("goto", "news");

        HashMap<String, String> body = new HashMap<>();
        body.put("goto", "news");
        body.put("acct", username);
        body.put("pw", password);

        GetHNUserTokenHTTPCommand command = new GetHNUserTokenHTTPCommand(
                url, queryParams, RequestType.POST, false, null, context, body);
        command.run();

        if (command.getErrorCode() != IAPICommand.ERROR_NONE) {
            throw new Exception("Login error code: " + command.getErrorCode());
        }

        return command.getResponseContent();
    }

    @Override
    public Boolean vote(String voteUrl, CookieStore cookieStore) throws Exception {
        HNVoteCommand command = new HNVoteCommand(
                voteUrl, null, RequestType.GET, false, null, context, cookieStore);
        command.run();

        if (command.getErrorCode() != IAPICommand.ERROR_NONE) {
            return false;
        }

        return command.getResponseContent();
    }
}
