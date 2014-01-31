package me.tatarka.ipromise;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
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
    public static <T> Promise<T> run(Task.Do<T> callback) {
        return Tasks.of(callback).start();
    }

    /**
     * Runs the callback with the given {@link java.util.concurrent.Executor}, delivering the result
     * in the returned {@link me.tatarka.ipromise.Promise}. This is equivalent to: {@code
     * Task.of(executor, callback).start()}.
     *
     * @param callback the callback
     * @param <T>      the result type
     * @return the promise that will deliver the result
     */
    public static <T> Promise<T> run(Executor executor, Task.Do<T> callback) {
        return Tasks.of(executor, callback).start();
    }

    /**
     * Runs the callback in a separate thread, delivering the result in the returned {@link
     * me.tatarka.ipromise.Promise}. This is equivalent to: {@code Task.ofCancelable(callback).start()}.
     *
     * @param callback the callback to run in a separate thread.
     * @param <T>      the result type
     * @return the promise that will deliver the result
     */
    public static <T> Promise<T> runCancelable(Task.Do<T> callback) {
        return Tasks.ofCancelable(callback).start();
    }

    /**
     * Runs the callback with the given {@link java.util.concurrent.Executor}, delivering the result
     * in the returned {@link me.tatarka.ipromise.Promise}. This is equivalent to: {@code
     * Task.ofCancelable(executor, callback).start()}.
     *
     * @param callback the callback
     * @param <T>      the result type
     * @return the promise that will deliver the result
     */
    public static <T> Promise<T> runCancelable(ExecutorService executor, Task.Do<T> callback) {
        return Tasks.ofCancelable(executor, callback).start();
    }

    /**
     * Constructs a new {@link me.tatarka.ipromise.ExecutorTask} that calls the given callback in a
     * separate thread when {@link Task#start()} is called.
     *
     * @param callback the callback to run in a separate thread
     * @param <T>      the result type
     * @return the promise that will deliver the result
     * @see me.tatarka.ipromise.ExecutorTask
     */
    public static <T> ExecutorTask<T> of(Task.Do<T> callback) {
        return Tasks.of(Executors.newSingleThreadExecutor(), callback);
    }

    /**
     * Constructs a new {@link me.tatarka.ipromise.ExecutorTask} that calls the given callback using
     * the given {@link java.util.concurrent.Executor} when {@link Task#start()} is called.
     *
     * @param executor the executor used to run the callback
     * @param callback the callback
     * @param <T>      the result type
     * @return the promise that will deliver the result
     * @see me.tatarka.ipromise.ExecutorTask
     */
    public static <T> ExecutorTask<T> of(Executor executor, Task.Do<T> callback) {
        return new ExecutorTask<T>(executor, callback);
    }

    /**
     * Constructs a new {@link me.tatarka.ipromise.CancelableExecutorTask} that calls the given
     * callback in a separate thread when {@link Task#start()} is called.
     *
     * @param callback the callback to run in a separate thread
     * @param <T>      the result type
     * @return the promise that will deliver the result
     * @see me.tatarka.ipromise.CancelableExecutorTask
     */
    public static <T> CancelableExecutorTask<T> ofCancelable(Task.Do<T> callback) {
        return new CancelableExecutorTask<T>(callback);
    }

    /**
     * Constructs a new {@link me.tatarka.ipromise.CancelableExecutorTask} that calls the given
     * callback using the given {@link java.util.concurrent.Executor} when {@link Task#start()} is
     * called.
     *
     * @param executor the executor used to run the callback
     * @param callback the callback
     * @param <T>      the result type
     * @return the promise that will deliver the result
     * @see me.tatarka.ipromise.CancelableExecutorTask
     */
    public static <T> CancelableExecutorTask<T> ofCancelable(ExecutorService executor, Task.Do<T> callback) {
        return new CancelableExecutorTask<T>(executor, callback);
    }

}
