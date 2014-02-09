package me.tatarka.ipromise;

import me.tatarka.ipromise.compat.Objects;

/**
 * An immutable result of either {@link Result#SUCCESS} or {@link Result#ERROR}.
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

    /**
     * Returns the result value if successful, otherwise throws an {@link
     * java.lang.IllegalStateException}.
     *
     * @return the result
     */
    public T getSuccess() {
        if (isSuccess()) {
            return success;
        } else {
            throw new IllegalStateException("Result was not successful");
        }
    }

    /**
     * Returns the error if there is one, otherwise throws an {@link java.lang.IllegalStateException}.
     *
     * @return the error
     */
    public E getError() {
        if (isError()) {
            return error;
        } else {
            throw new IllegalStateException("Result was not an onError");
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

    /**
     * Constructs a new result that has the value of this result passed through the given map
     * function on success, and the same error on failure.
     *
     * @param map  the function to pass a successful result through
     * @param <T2> the type of the new successful result
     * @return the new result
     */
    public <T2> Result<T2, E> onSuccess(me.tatarka.ipromise.func.Map<T, T2> map) {
        if (isSuccess()) {
            return Result.success(map.map(getSuccess()));
        } else {
            return (Result<T2, E>) this;
        }
    }

    /**
     * Constructs a new result that has the value of this result passed through the given map
     * function on error, and the same value on success.
     *
     * @param map  the function to pass an error result through
     * @param <E2> the type of the new error result
     * @return the new result
     */
    public <E2 extends Exception> Result<T, E2> onError(me.tatarka.ipromise.func.Map<E, E2> map) {
        if (isError()) {
            return Result.error(map.map(getError()));
        } else {
            return (Result<T, E2>) this;
        }
    }

    /**
     * Constructs a new result that has the value of the chain function if the first result is
     * successful, otherwise it has the error of the first result.
     *
     * @param chain the chain function
     * @param <T2>  the type of the second result
     * @return the new result
     */
    public <T2> Result<T2, E> onSuccess(me.tatarka.ipromise.func.Chain<T, Result<T2, E>> chain) {
        if (isSuccess()) {
            return chain.chain(getSuccess());
        } else {
            return (Result<T2, E>) this;
        }
    }

    /**
     * Constructs a new result that has the value of teh chain function if the first result is an
     * error, otherwise it has the value of the first result.
     *
     * @param chain the chain function
     * @param <E2>  the error type of the second result
     * @return the new result
     */
    public <E2 extends Exception> Result<T, E2> onError(me.tatarka.ipromise.func.Chain<E, Result<T, E2>> chain) {
        if (isError()) {
            return chain.chain(getError());
        } else {
            return (Result<T, E2>) this;
        }
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

    /**
     * A listener to use for {@link Promise#listen(me.tatarka.ipromise.Listener)} or that gives two
     * callbacks for success and failure instead of just one. You may override one or both.
     *
     * @param <T> the success type
     * @param <E> the error type
     */
    public static abstract class Listener<T, E extends Exception> implements me.tatarka.ipromise.Listener<Result<T, E>> {
        @Override
        public final void receive(Result<T, E> message) {
            if (message.isSuccess()) {
                success(message.getSuccess());
            } else {
                error(message.getError());
            }
        }

        /**
         * Called when the result is successful.
         *
         * @param success the value of the result
         */
        protected void success(T success) {
        }

        /**
         * Called when the result is an error.
         *
         * @param error the error of the result
         */
        protected void error(E error) {
        }
    }

    /**
     * A listener to use for {@link Promise#then(me.tatarka.ipromise.func.Map)} that gives two
     * callbacks for success and failure instead of just one.
     *
     * @param <T1> the type of the original result success value
     * @param <T2> the type of the new result success value
     * @param <E>  the type of the result error
     */
    public static abstract class Map<T1, T2, E extends Exception> implements me.tatarka.ipromise.func.Map<Result<T1, E>, Result<T2, E>> {
        @Override
        public final Result<T2, E> map(Result<T1, E> result) {
            if (result.isSuccess()) {
                return Result.success(success(result.getSuccess()));
            } else {
                return Result.error(error(result.getError()));
            }
        }

        /**
         * Map the success value of the result to a new success value of another type.
         *
         * @param success the success value
         * @return the new result value
         */
        protected abstract T2 success(T1 success);

        /**
         * Optionally override to change the result error value. By default, it is simply returned
         * unchanged.
         *
         * @param error the result error
         * @return the new error
         */
        protected E error(E error) {
            return error;
        }
    }

    /**
     * A listener to use for {@link Promise#then(me.tatarka.ipromise.func.Chain)} that gives two
     * callbacks for success and failure. With this class you must implement them both. If you don't
     * need to change the error value, you should use the more specific subclasses {@link
     * Result.ChainResult} and {@link Result.ChainPromise}.
     *
     * @param <T1> the result success type
     * @param <E>  the result error type
     * @param <R>  the return type
     */
    public static abstract class Chain<T1, E extends Exception, R> implements me.tatarka.ipromise.func.Chain<Result<T1, E>, R> {
        @Override
        public final R chain(Result<T1, E> result) {
            if (result.isSuccess()) {
                return success(result.getSuccess());
            } else {
                return error(result.getError());
            }
        }

        /**
         * Called when the result has a successful value
         *
         * @param success the value
         * @return the new chained value
         */
        protected abstract R success(T1 success);

        /**
         * Called when the result has an error value
         *
         * @param error the error
         * @return the new chained value
         */
        protected abstract R error(E error);
    }

    /**
     * A listener to use for {@link Result#onSuccess(me.tatarka.ipromise.func.Chain)} that gives you
     * a callback for a successful value.
     *
     * @param <T1> the type of the original result  success value
     * @param <T2> the type of the new result success value
     * @param <E>  the error type
     * @see Result.Chain
     */
    public static abstract class ChainResult<T1, T2, E extends Exception> extends Chain<T1, E, Result<T2, E>> {
        @Override
        protected Result<T2, E> error(E error) {
            return Result.error(error);
        }
    }

    /**
     * A listener to use for {@link Promise#then(me.tatarka.ipromise.func.Chain)} that gives you a
     * callback for a successful value.
     *
     * @param <T1> the type of the original result  success value
     * @param <T2> the type of the new result success value
     * @param <E>  the error type
     * @see Result.Chain
     */
    public static abstract class ChainPromise<T1, T2, E extends Exception> extends Chain<T1, E, Promise<Result<T2, E>>> {
        private Deferred.Builder deferredBuilder;

        public ChainPromise() {
            this(new Deferred.Builder());
        }

        public ChainPromise(Deferred.Builder deferredBuilder) {
            this.deferredBuilder = deferredBuilder;
        }

        @Override
        protected Promise<Result<T2, E>> error(E error) {
            return deferredBuilder.<Result<T2, E>>build().resolve(Result.<T2, E>error(error)).promise();
        }
    }
}
