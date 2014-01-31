package me.tatarka.ipromise;

import java.util.concurrent.ExecutorService;

/**
 * An {@code ExecutorTask} the implements a simple method of cancellation. Specifically, it will
 * call {@link java.util.concurrent.ExecutorService#shutdownNow()}. In most cases this will
 * interrupt the thread the callback is running on.
 *
 * @param <T> the result type
 * @see me.tatarka.ipromise.Task
 */
public class CancelableExecutorTask<T> extends ExecutorTask<T> {
    /**
     * Constructs a new {@code Task} that will run the given callback in a separate thread.
     *
     * @param callback the callback
     */
    public CancelableExecutorTask(Do<T> callback) {
        super(callback);
    }

    /**
     * Constructs a new {@code Task} that will run the given callback with the given {@link
     * java.util.concurrent.ExecutorService}.
     *
     * @param executor the executor service to run the callback
     * @param callback the callback
     */
    public CancelableExecutorTask(ExecutorService executor, Do<T> callback) {
        super(executor, callback);
    }

    @Override
    public Promise<T> start() {
        cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                ((ExecutorService) executor).shutdownNow();
            }
        });
        return super.start();
    }
}
