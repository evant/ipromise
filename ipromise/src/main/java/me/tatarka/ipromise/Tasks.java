package me.tatarka.ipromise;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A collection of helper methods for constructing tasks.
 */
public final class Tasks {
    private Tasks() {
    }

    /**
     * Runs the callback in a separate thread, delivering the result in the returned {@link
     * me.tatarka.ipromise.Promise}. This is equivalent to: {@code Task.of(callback).start()}.
     *
     * @param callback the callback to run in a separate thread.
     * @param <T>      the result type
     * @return the promise that will deliver the result
     */
    public static <T> Promise<T> run(PromiseTask.Do<T> callback) {
        return Tasks.of(callback).start();
    }

    /**
     * Runs the callback with the given {@link java.util.concurrent.Executor}, delivering the result
     * in the returned {@link me.tatarka.ipromise.Promise}. This is equivalent to: {@code
     * Task.of(executor, callback).start()}.
     *
     * @param executor the executor used to run the callback
     * @param callback the callback
     * @param <T>      the result type
     * @return the promise that will deliver the result
     */
    public static <T> Promise<T> run(Executor executor, PromiseTask.Do<T> callback) {
        return Tasks.of(executor, callback).start();
    }

    /**
     * Constructs a new {@link PromiseExecutorTask} that calls the given callback in a
     * separate thread when {@link Task#start()} is called.
     *
     * @param callback the callback to run in a separate thread
     * @param <T>      the result type
     * @return the promise that will deliver the result
     * @see PromiseExecutorTask
     */
    public static <T> PromiseExecutorTask<T> of(PromiseTask.Do<T> callback) {
        return Tasks.of(Executors.newSingleThreadExecutor(), callback);
    }

    /**
     * Constructs a new {@link PromiseExecutorTask} that calls the given callback using
     * the given {@link java.util.concurrent.Executor} when {@link Task#start()} is called.
     *
     * @param executor the executor used to run the callback
     * @param callback the callback
     * @param <T>      the result type
     * @return the promise that will deliver the result
     * @see PromiseExecutorTask
     */
    public static <T> PromiseExecutorTask<T> of(Executor executor, PromiseTask.Do<T> callback) {
        return new PromiseExecutorTask<T>(executor, callback);
    }

    public static <T> ProgressExecutorTask<T> of(ProgressTask.Do<T> callback) {
        return new ProgressExecutorTask<T>(callback);
    }

    public static <T> ProgressExecutorTask<T> of(Executor executor, ProgressTask.Do<T> callback) {
        return new ProgressExecutorTask<T>(executor, callback);
    }
}
