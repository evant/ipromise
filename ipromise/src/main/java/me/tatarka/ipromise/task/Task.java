package me.tatarka.ipromise.task;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Result;

/**
 * A way to control an async operation.
 *
 * @param <T> the result type
 */
public interface Task<T> {
    /**
     * Starts the async operation. This method is expected to return immediately, running the
     * operation asynchronously.
     *
     * @return an {@link me.tatarka.ipromise.Async} to manage the operation's result.
     */
    Promise<T> start();

    public interface Do<T> {
        void run(Deferred<T> deferred, CancelToken cancelToken);
    }

    public static abstract class DoOnce<T> implements Do<T> {
        @Override
        public final void run(Deferred<T> deferred, CancelToken cancelToken) {
            deferred.resolve(runOnce(cancelToken));
        }

        public abstract T runOnce(CancelToken cancelToken);
    }

    public static abstract class DoOnceFailable<T, E extends Exception> implements Do<Result<T, E>> {
        @Override
        public final void run(Deferred<Result<T, E>> deferred, CancelToken cancelToken) {
            try {
                deferred.resolve(Result.<T, E>success(runFailable(cancelToken)));
            } catch (Exception e) {
                deferred.resolve(Result.<T, E>error((E) e));
            }
        }

        public abstract T runFailable(CancelToken cancelToken) throws E;
    }
}
