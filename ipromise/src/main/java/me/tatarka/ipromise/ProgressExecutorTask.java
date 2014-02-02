package me.tatarka.ipromise;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * An Task that runs a progress callback using an {@link java.util.concurrent.Executor}.
 *
 * @author Evan Tatarka
 * @see me.tatarka.ipromise.Task
 * @see me.tatarka.ipromise.ProgressTask
 */
public class ProgressExecutorTask<T> implements ProgressTask<T> {
    protected Executor executor;
    protected Do<T> callback;

    /**
     * Creates a new {@code Task} that runs the given callback in a new thread.
     *
     * @param callback the callback
     */
    public ProgressExecutorTask(Do<T> callback) {
        this(Executors.newSingleThreadExecutor(), callback);
    }

    /**
     * Creates a new {@code Task} that runs the given callback with the given {@link
     * java.util.concurrent.Executor}.
     *
     * @param executor the executor
     * @param callback the callback
     */
    public ProgressExecutorTask(Executor executor, Do<T> callback) {
        this.executor = executor;
        this.callback = callback;
    }

    @Override
    public Progress<T> start() {
        final CancelToken cancelToken = new CancelToken();
        final Channel<T> channel = new Channel<T>(cancelToken);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                callback.run(channel, cancelToken);
            }
        });
        return channel.progress();
    }
}
