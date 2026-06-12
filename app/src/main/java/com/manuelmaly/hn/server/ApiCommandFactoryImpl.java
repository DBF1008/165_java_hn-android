package com.manuelmaly.hn.server;

import android.app.Application;

import com.manuelmaly.hn.server.IAPICommand.RequestType;

import cz.msebera.android.httpclient.client.CookieStore;

import java.io.Serializable;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Default {@link ApiCommandFactory}. Builds the same concrete commands the tasks
 * used to {@code new} directly (identical URLs, timeouts and flags), and injects
 * the {@link IHttpClientProvider} / {@link INetworkStatus} seams into each.
 */
@Singleton
public class ApiCommandFactoryImpl implements ApiCommandFactory {

    private static final String COMMENTS_URL = "https://news.ycombinator.com/item";
    private static final String LOGIN_URL = "https://news.ycombinator.com/login";

    private final Application mApp;
    private final IHttpClientProvider mHttpClientProvider;
    private final INetworkStatus mNetworkStatus;

    @Inject
    public ApiCommandFactoryImpl(Application app, IHttpClientProvider httpClientProvider, INetworkStatus networkStatus) {
        mApp = app;
        mHttpClientProvider = httpClientProvider;
        mNetworkStatus = networkStatus;
    }

    @Override
    public IAPICommand<String> createFeedDownload(String url, CookieStore cookieStore) {
        return configure(new StringDownloadCommand(url, new HashMap<String, String>(), RequestType.GET, false, null,
            mApp, cookieStore));
    }

    @Override
    public IAPICommand<String> createCommentsDownload(String postId, CookieStore cookieStore) {
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("id", postId);
        return configure(new StringDownloadCommand(COMMENTS_URL, queryParams, RequestType.GET, false, null,
            mApp, cookieStore));
    }

    @Override
    public IAPICommand<Boolean> createVote(String voteUrl, CookieStore cookieStore) {
        return configure(new HNVoteCommand(voteUrl, null, RequestType.GET, false, null, mApp, cookieStore));
    }

    @Override
    public IAPICommand<String> createLoginToken(String username, String password) {
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("goto", "news");
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("goto", "news");
        body.put("acct", username);
        body.put("pw", password);
        return configure(new GetHNUserTokenHTTPCommand(LOGIN_URL, queryParams, RequestType.POST, false, null, mApp, body));
    }

    private <T extends Serializable> BaseHTTPCommand<T> configure(BaseHTTPCommand<T> command) {
        command.setHttpClientProvider(mHttpClientProvider);
        command.setNetworkStatus(mNetworkStatus);
        return command;
    }

}
