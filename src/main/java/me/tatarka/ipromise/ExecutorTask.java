package me.tatarka.ipromise;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A task that uses an {@link java.util.concurrent.Executor} to run the given callback.
 *
 * @param <T> the result type
 */
public class ExecutorTask<T> implements Task<T> {
    protected Executor executor;
    protected Task.Do<T> callback;
    protected CancelToken cancelToken;
    protected Deferred<T> deferred;

    /**
     * Constructs a new {@code Task} that will run the given callback in a separate thread.
     *
     * @param callback the callback
     */
    public ExecutorTask(Task.Do<T> callback) {
        this(Executors.newSingleThreadExecutor(), callback);
    }

    /**
     * Constructs a new {@code Task} that will run the given callback with the given executor.
     *
     * @param executor the executor to run the callback
     * @param callback the callback
     */
    public ExecutorTask(Executor executor, Task.Do<T> callback) {
        cancelToken = new CancelToken();
        deferred = new Deferred<T>(cancelToken);
        this.executor = executor;
        this.callback = callback;
    }

    /**
     * Returns the {@link me.tatarka.ipromise.Promise}
     *
     * @return the promise.
     */
    public Promise<T> promise() {
        return deferred.promise();
    }

    @Override
    public Promise<T> start() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                deferred.resolve(callback.run(cancelToken));
            }
        });
        return deferred.promise();
    }
}
