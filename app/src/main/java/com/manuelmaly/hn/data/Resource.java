package com.manuelmaly.hn.data;

/**
 * A generic wrapper for data loaded by the repository, providing status information.
 * Used with LiveData to communicate loading/success/error states to the UI layer.
 *
 * @param <T> the type of data being wrapped
 */
public class Resource<T> {

    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    public final Status status;
    public final T data;
    public final String message;

    private Resource(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(String message, T data) {
        return new Resource<>(Status.ERROR, data, message);
    }

    public static <T> Resource<T> loading(T data) {
        return new Resource<>(Status.LOADING, data, null);
    }
}
