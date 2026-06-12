package com.manuelmaly.hn.data;

import com.manuelmaly.hn.server.IAPICommand;

/**
 * Thrown by {@link IHNRemoteDataSource} when a network/parse call fails, carrying
 * the underlying {@link IAPICommand} error code so the repository can map it onto
 * {@link Resource#error(int, Object)}.
 */
public class HNApiException extends Exception {

    private final int mErrorCode;

    public HNApiException(int errorCode) {
        super("HN API call failed with error code " + errorCode);
        mErrorCode = errorCode;
    }

    public int getErrorCode() {
        return mErrorCode;
    }
}
