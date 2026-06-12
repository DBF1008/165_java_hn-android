package com.manuelmaly.hn.server;

import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

/**
 * Creates the {@link DefaultHttpClient} used by {@link BaseHTTPCommand}. Extracted
 * so the hard-coded {@code new DefaultHttpClient(...)} inside the command becomes a
 * replaceable seam. Returns the concrete type so subclasses can still call
 * {@code modifyHttpClient(DefaultHttpClient)}.
 */
public interface IHttpClientProvider {

    DefaultHttpClient create(int connectionTimeoutMs, int socketTimeoutMs);

}
