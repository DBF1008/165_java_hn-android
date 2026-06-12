package com.manuelmaly.hn.server;

import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.params.BasicHttpParams;
import cz.msebera.android.httpclient.params.HttpConnectionParams;
import cz.msebera.android.httpclient.params.HttpParams;

import javax.inject.Inject;

/**
 * Default {@link IHttpClientProvider} — reproduces the {@code BasicHttpParams} +
 * {@code new DefaultHttpClient(...)} that {@link BaseHTTPCommand} used inline.
 */
public class DefaultHttpClientProvider implements IHttpClientProvider {

    @Inject
    public DefaultHttpClientProvider() {
    }

    @Override
    public DefaultHttpClient create(int connectionTimeoutMs, int socketTimeoutMs) {
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeoutMs);
        HttpConnectionParams.setSoTimeout(httpParameters, socketTimeoutMs);
        return new DefaultHttpClient(httpParameters);
    }

}
