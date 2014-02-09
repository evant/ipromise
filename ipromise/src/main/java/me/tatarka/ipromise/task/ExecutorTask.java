package me.tatarka.ipromise.task;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.task.Task;

/**
 * An Task that runs a progress callback using an {@link java.util.concurrent.Executor}.
 *
 * @author Evan Tatarka
 * @see me.tatarka.ipromise.task.Task
 */
public class ExecutorTask<T> implements Task<T> {
    protected Deferred.Builder deferredBuilder;
    protected Executor executor;
    protected Do<T> callback;

    /**
     * Creates a new {@code Task} that runs the given callback in a new thread.
     *
     * @param callback the callback
     */
    public ExecutorTask(Do<T> callback) {
        this(new Deferred.Builder(), Executors.newSingleThreadExecutor(), callback);
    }

    public ExecutorTask(Executor executor, Do<T> callback) {
        this(new Deferred.Builder(), executor, callback);
    }

    /**
     * Creates a new {@code Task} that runs the given callback with the given {@link
     * java.util.concurrent.Executor}.
     *
     * @param executor the executor
     * @param callback the callback
     */
    public ExecutorTask(Deferred.Builder deferredBuilder, Executor executor, Do<T> callback) {
        this.deferredBuilder = deferredBuilder;
        this.executor = executor;
        this.callback = callback;
    }

    @Override
    public Promise<T> start() {
        final CancelToken cancelToken = new CancelToken();
        final Deferred<T> deferred = deferredBuilder.build(cancelToken);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                callback.run(deferred, cancelToken);
            }
        });
        return deferred.promise();
    }
}
