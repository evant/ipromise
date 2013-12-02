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
     * Retrieves the value from the result in a safe way. If the result is successful, returns the
     * result's value. If the result is an error, it throws the result's Exception.
     *
     * @return the value of the result if successful
     * @throws E thrown on an error result
     */
    public T get() throws E {
        if (isSuccess()) {
            return success;
        } else {
            throw error;
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

    public T getSuccess() {
        if (isSuccess()) {
            return success;
        } else {
            throw new IllegalStateException("Result was not successful");
        }
    }

    public E getError() {
        if (isError()) {
            return error;
        } else {
            throw new IllegalStateException("Result was not an error");
        }
    }

    /**
     * Returns if the result is an error
     *
     * @return true if an error, false otherwise
     */
    public boolean isError() {
        return state == ERROR;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(getClass())) return false;
        Result other = (Result) o;

        if (state == SUCCESS) {
            return other.state == SUCCESS && Objects.equals(success, other.success);
        } else {
            return other.state == ERROR && Objects.equals(error, other.error);
        }
    }

    @Override
    public int hashCode() {
        if (state == SUCCESS) {
            return Objects.hashCode(success);
        } else {
            return Objects.hashCode(error);
        }
    }

    @Override
    public String toString() {
        if (state == SUCCESS) {
            return "Result<Success>(" + success + ")";
        } else {
            return "Result<Error>(" + error + ")";
        }
    }

    public static abstract class Listener<T, E extends Exception> implements me.tatarka.ipromise.Listener<Result<T, E>> {
        @Override
        public final void receive(Result<T, E> result) {
            if (result.isSuccess()) {
                success(result.getSuccess());
            } else {
                error(result.getError());
            }
        }

        protected void success(T success) {}
        protected void error(E error) {}
    }

    public static abstract class Map<T1, T2, E extends Exception> implements me.tatarka.ipromise.Map<Result<T1, E>, Result<T2, E>> {
        @Override
        public final Result<T2, E> map(Result<T1, E> result) {
            if (result.isSuccess()) {
                return Result.success(success(result.getSuccess()));
            } else {
                return Result.error(error(result.getError()));
            }
        }

        protected abstract T2 success(T1 success);

        protected E error(E error) {
            return error;
        }
    }

    public static abstract class Chain<T1, E extends Exception, R> implements me.tatarka.ipromise.Chain<Result<T1, E>, R> {
        @Override
        public final R chain(Result<T1, E> result) {
            if (result.isSuccess()) {
                return success(result.getSuccess());
            } else {
                return error(result.getError());
            }
        }

        protected abstract R success(T1 success);
        protected abstract R error(E error);
    }

    public static abstract class ChainPromise<T1, T2, E extends Exception> extends Chain<T1, E, Promise<Result<T2, E>>> {
        @Override
        protected Promise<Result<T2, E>>  error(E error) {
            return new Promise<Result<T2, E>>(Result.<T2, E>error(error));
        }
    }

    public static abstract class ChainProgress<T1, T2, E extends Exception> extends Chain<T1, E, Progress<Result<T2, E>>> {
        @Override
        protected Progress<Result<T2, E>>  error(E error) {
            return new Progress<Result<T2, E>>(Result.<T2, E>error(error));
        }
    }
}
