package com.manuelmaly.hn.data;

import com.manuelmaly.hn.server.IAPICommand;

/**
 * Immutable wrapper describing the state of a piece of data that is loaded
 * asynchronously: LOADING / SUCCESS / ERROR, the (possibly stale) data, and an
 * error code from {@link IAPICommand}.
 *
 * <p>This is the unit the repositories expose to the ViewModels via LiveData,
 * replacing the old {@code ITaskFinishedHandler.TaskResultCode} + raw result
 * callback.</p>
 */
public class Resource<T> {

    public enum Status {
        LOADING, SUCCESS, ERROR
    }

    private final Status mStatus;
    private final T mData;
    private final int mErrorCode;

    private Resource(Status status, T data, int errorCode) {
        mStatus = status;
        mData = data;
        mErrorCode = errorCode;
    }

    /** In-progress load; {@code data} is whatever we currently have (may be null). */
    public static <T> Resource<T> loading(T data) {
        return new Resource<T>(Status.LOADING, data, IAPICommand.ERROR_NONE);
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<T>(Status.SUCCESS, data, IAPICommand.ERROR_NONE);
    }

    /** Failed load; {@code data} is the previous value to keep showing (may be null). */
    public static <T> Resource<T> error(int errorCode, T data) {
        return new Resource<T>(Status.ERROR, data, errorCode);
    }

    public Status getStatus() {
        return mStatus;
    }

    public T getData() {
        return mData;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public boolean isLoading() {
        return mStatus == Status.LOADING;
    }

    public boolean isSuccess() {
        return mStatus == Status.SUCCESS;
    }

    public boolean isError() {
        return mStatus == Status.ERROR;
    }

    @Override
    public String toString() {
        return "Resource{status=" + mStatus + ", errorCode=" + mErrorCode
                + ", data=" + mData + "}";
    }
}
