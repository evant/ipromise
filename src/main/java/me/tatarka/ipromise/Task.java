package me.tatarka.ipromise;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * An easy way to start run something asynchronously and return a {@link
 * me.tatarka.ipromise.Promise}.
 *
 * @param <T> the result type
 */
public class Task<T> {
    protected CancelToken cancelToken;
    protected Deferred<T> deferred;
    protected Executor executor;
    protected Do<T> callback;

    /**
     * Runs the callback in a separate thread, delivering the result in the returned {@link
     * me.tatarka.ipromise.Promise}. This is equivalent to: {@code new Task(callback).start()}.
     *
     * @param callback the callback to run in a separate thread.
     * @param <T>      the result type
     * @return the promise that will deliver the result
     */
    public static <T> Promise<T> run(Do<T> callback) {
        return new Task<T>(callback).start();
    }

    /**
     * Runs the callback with the given {@link java.util.concurrent.Executor}, delivering the result
     * in the returned {@link me.tatarka.ipromise.Promise}. This is equivalent to: {@code new
     * Task(executor, callback).start()}.
     *
     * @param executor the executor to use to run the callback.
     * @param callback the callback
     * @param <T>      the result type
     * @return the promise that will deliver the result
     */
    public static <T> Promise<T> run(Executor executor, Do<T> callback) {
        return new Task<T>(executor, callback).start();
    }

    /**
     * Constructs a new {@code Task} with the given callback that will be run in a separate thread
     * when {@link Task#start()} is called.
     *
     * @param callback the callback to run in a separate thread
     */
    public Task(Do<T> callback) {
        this(Executors.newSingleThreadExecutor(), callback);
    }

    /**
     * Constructs a new {@code Task} with the given callback that will be run by the given {@link
     * java.util.concurrent.Executor} when {@link Task#start()} is called.
     *
     * @param executor the executor to use to run the callback.
     * @param callback the callback
     */
    public Task(Executor executor, Do<T> callback) {
        cancelToken = new CancelToken();
        deferred = new Deferred<T>(cancelToken);
        this.executor = executor;
        this.callback = callback;
    }

    /**
     * Returns the promise for this {@code Task}.
     *
     * @return the promise
     */
    public Promise<T> promise() {
        return deferred.promise();
    }

    /**
     * Start executing the callback for this {@code Task}.
     *
     * @return the promise
     */
    public Promise<T> start() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                deferred.resolve(callback.run(cancelToken));
            }
        });
        return promise();
    }

    /**
     * The callback for the task to execute
     *
     * @param <T> the result type
     */
    public static interface Do<T> {
        T run(CancelToken cancelToken);
    }

    /**
     * If your callback may throw an exception, you can use this to automatically, catch the
     * exception and return a {@link me.tatarka.ipromise.Result}.
     *
     * @param <T> the success type
     * @param <E> the error type
     */
    public static abstract class DoFailable<T, E extends Exception> implements Do<Result<T, E>> {
        @Override
        public final Result<T, E> run(CancelToken cancelToken) {
            try {
                return Result.success(runFailable(cancelToken));
            } catch (Exception e) {
                return Result.error((E) e);
            }
        }

        public abstract T runFailable(CancelToken cancelToken) throws E;
    }
}
