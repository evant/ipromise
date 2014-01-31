package me.tatarka.ipromise;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A task that uses an {@link java.util.concurrent.Executor} to run the given callback.
 *
 * @param <T> the result type
 */
public class PromiseExecutorTask<T> implements PromiseTask<T> {
    protected Executor executor;
    protected Do<T> callback;

    /**
     * Constructs a new {@code Task} that will run the given callback in a separate thread.
     *
     * @param callback the callback
     */
    public PromiseExecutorTask(Do<T> callback) {
        this(Executors.newSingleThreadExecutor(), callback);
    }

    /**
     * Constructs a new {@code Task} that will run the given callback with the given executor.
     *
     * @param executor the executor to run the callback
     * @param callback the callback
     */
    public PromiseExecutorTask(Executor executor, Do<T> callback) {
        this.executor = executor;
        this.callback = callback;
    }

    @Override
    public Promise<T> start() {
        final CancelToken cancelToken = new CancelToken();
        final Deferred<T> deferred = new Deferred<T>(cancelToken);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                deferred.resolve(callback.run(cancelToken));
            }
        });
        return deferred.promise();
    }
}
