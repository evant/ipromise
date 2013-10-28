package me.tatarka.ipromise;

import java.util.Objects;

/**
 * An immutable result from a promise. The result can be of either type success, error, or canceled.
 * This class uses checked exceptions to ensure safety when retrieving the result.
 *
 * @author Evan Tatarka
 */
public final class Result<T, E extends Exception> {
    /**
     * An error result.
     */
    public static final int ERROR = 0;

    /**
     * A successful result.
     */
    public static final int SUCCESS = 1;

    /**
     * A canceled result.
     */
    public static final int CANCELED = 2;

    // Because this class is immutable and a canceled result has no data, a singleton instance can be used for all canceled results.
    private static final Result CANCEL_RESULT = new Result();

    private final int state;
    private final T success;
    private final E error;

    /**
     * Constructs a new successful result with the given value.
     *
     * @param success the result's value
     * @param <T>     the type of the result's value
     * @param <E>     teh type of the result's error value
     * @return the new result
     */
    public static <T, E extends Exception> Result<T, E> success(T success) {
        return new Result<T, E>(success);
    }

    private Result(T result) {
        state = SUCCESS;
        success = result;
        error = null;
    }

    /**
     * Constructs a new error result with the given Exception.
     *
     * @param error teh result's error
     * @param <T>   the type of the result's value
     * @param <E>   teh type of the result's error value
     * @return the new result
     */
    public static <T, E extends Exception> Result<T, E> error(E error) {
        return new Result<T, E>(error);
    }

    private Result(E result) {
        state = ERROR;
        success = null;
        error = result;
    }

    /**
     * Constructs a new cancel result.
     *
     * @param <T> the type of the result's value
     * @param <E> teh type of the result's error value
     * @return the new result
     */
    public static <T, E extends Exception> Result<T, E> cancel() {
        return CANCEL_RESULT;
    }

    private Result() {
        state = CANCELED;
        success = null;
        error = null;
    }

    /**
     * Retrieves the value from the result in a safe way. If the result is successful, returns the
     * result's value. If the result is an error, it throws the result's Exception. If the result if
     * canceled, it throws a {@link Result.CanceledException}.
     *
     * @return the value of the result if successful
     * @throws E                 thrown on an error result
     * @throws CanceledException thrown on a cancel result
     */
    public T get() throws E, CanceledException {
        if (isSuccess()) {
            return success;
        } else if (isError()) {
            throw error;
        } else {
            throw new CanceledException(this);
        }
    }

    /**
     * Returns if the result is successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return state == SUCCESS;
    }

    /**
     * Returns if the result is an error
     *
     * @return true if an error, false otherwise
     */
    public boolean isError() {
        return state == ERROR;
    }

    /**
     * Returns if the result is canceled
     *
     * @return true if canceled, false otherwise
     */
    public boolean isCanceled() {
        return state == CANCELED;
    }

    /**
     * The exception thrown when a result is canceled.
     */
    public static class CanceledException extends Exception {
        public CanceledException(Result result) {
            super(result + " was canceled");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(getClass())) return false;
        Result other = (Result) o;

        if (state == SUCCESS) {
            return other.state == SUCCESS && Objects.equals(success, other.success);
        } else if (state == ERROR) {
            return other.state == ERROR && Objects.equals(error, other.error);
        } else {
            return other.state == CANCELED;
        }
    }

    @Override
    public int hashCode() {
        if (state == SUCCESS) {
            return Objects.hashCode(success);
        } else if (state == ERROR) {
            return Objects.hashCode(error);
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        if (state == SUCCESS) {
            return "Result<Success>(" + success + ")";
        } else if (state == ERROR) {
            return "Result<Error>(" + error + ")";
        } else {
            return "Result<Cancel>";
        }
    }
}
