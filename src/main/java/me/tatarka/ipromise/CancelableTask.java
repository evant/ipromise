package me.tatarka.ipromise;

import java.util.concurrent.ExecutorService;

/**
 * A {@code Task} the implements a simple method of cancellation. Specifically, it will call {@link
 * java.util.concurrent.ExecutorService#shutdownNow()}. In most cases this will interrupt the thread
 * the callback is running on.
 *
 * @param <T> the result type
 * @see me.tatarka.ipromise.Task
 */
public class CancelableTask<T> extends Task<T> {
    /**
     * Runs the callback in a separate thread, delivering the result in the returned {@link
     * me.tatarka.ipromise.Promise}. This is equivalent to: {@code new
     * CancelableTask(callback).start()}.
     *
     * @param callback the callback to run in a separate thread.
     * @param <T>      the result type
     * @return the promise that will deliver the result
     */
    public static <T> Promise<T> run(Do<T> callback) {
        return new CancelableTask<T>(callback).start();
    }

    /**
     * Runs the callback with the given {@link java.util.concurrent.Executor}, delivering the result
     * in the returned {@link me.tatarka.ipromise.Promise}. This is equivalent to: {@code new
     * CancelableTask(executor, callback).start()}.
     *
     * @param executor the executor to use to run the callback.
     * @param callback the callback
     * @param <T>      the result type
     * @return the promise that will deliver the result
     */
    public static <T> Promise<T> run(ExecutorService executor, Do<T> callback) {
        return new CancelableTask<T>(executor, callback).start();
    }

    /**
     * Constructs a new {@code CancelableTask} with the given callback that will be run in a
     * separate thread when {@link CancelableTask#start()} is called.
     *
     * @param callback the callback to run in a separate thread
     */
    public CancelableTask(Do<T> callback) {
        super(callback);
    }

    /**
     * Constructs a new {@code Task} with the given callback that will be run by the given {@link
     * java.util.concurrent.Executor} when {@link CancelableTask#start()} is called.
     *
     * @param executor the executor to use to run the callback.
     * @param callback the callback
     */
    public CancelableTask(ExecutorService executor, Do<T> callback) {
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
