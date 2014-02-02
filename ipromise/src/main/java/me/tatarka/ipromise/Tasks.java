package me.tatarka.ipromise;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A collection of helper methods for constructing tasks.
 *
 * @author Evan Tatarka
 */
public final class Tasks {
    private Tasks() {
    }

    /**
     * Runs the callback in a separate thread, delivering the result in the returned {@link
     * me.tatarka.ipromise.Promise}. This is equivalent to: {@code Task.of(callback).start()}.
     *
     * @param callback the callback to run in a separate thread
     * @param <T>      the result type
     * @return the promise that will receive the result
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
     * @return the promise that will receive the result
     */
    public static <T> Promise<T> run(Executor executor, PromiseTask.Do<T> callback) {
        return Tasks.of(executor, callback).start();
    }

    /**
     * Runs the callback in a separate thread, delivering the messages to the returned {@link
     * me.tatarka.ipromise.Progress}. This is equivalent to: {@code Task.of(callback).start()}.
     *
     * @param callback the callback to run in a separate thread
     * @param <T>      the message type
     * @return the progress that will receive the messages
     */
    public static <T> Progress<T> run(ProgressTask.Do<T> callback) {
        return Tasks.of(callback).start();
    }

    /**
     * Runs the callback with the given {@link java.util.concurrent.Executor}, delivering messages
     * to the returned {@link me.tatarka.ipromise.Progress}. This is equivalent to: {@code
     * Task.of(executor, callback).start()}.
     *
     * @param executor the executor used to run the callback
     * @param callback teh callback
     * @param <T>      the message type
     * @return the progress that will receive the messages
     */
    public static <T> Progress<T> run(Executor executor, ProgressTask.Do<T> callback) {
        return Tasks.of(executor, callback).start();
    }

    /**
     * Constructs a new {@link PromiseExecutorTask} that calls the given callback in a separate
     * thread when {@link Task#start()} is called.
     *
     * @param callback the callback to run in a separate thread
     * @param <T>      the result type
     * @return the task
     * @see PromiseExecutorTask
     */
    public static <T> PromiseExecutorTask<T> of(PromiseTask.Do<T> callback) {
        return Tasks.of(Executors.newSingleThreadExecutor(), callback);
    }

    /**
     * Constructs a new {@link PromiseExecutorTask} that calls the given callback using the given
     * {@link java.util.concurrent.Executor} when {@link Task#start()} is called.
     *
     * @param executor the executor used to run the callback
     * @param callback the callback
     * @param <T>      the result type
     * @return the task
     * @see PromiseExecutorTask
     */
    public static <T> PromiseExecutorTask<T> of(Executor executor, PromiseTask.Do<T> callback) {
        return new PromiseExecutorTask<T>(executor, callback);
    }

    /**
     * Constructs a new {@link me.tatarka.ipromise.ProgressExecutorTask} that calls the given
     * callback in a separate thread.
     *
     * @param callback the callback
     * @param <T>      the message type
     * @return the task
     */
    public static <T> ProgressExecutorTask<T> of(ProgressTask.Do<T> callback) {
        return new ProgressExecutorTask<T>(callback);
    }

    /**
     * Constructs a new {@link me.tatarka.ipromise.ProgressExecutorTask} that calls the given
     * callback using the given {@link java.util.concurrent.Executor}.
     *
     * @param executor the executor to run the callback with
     * @param callback the callback
     * @param <T>      the message type
     * @return teh task
     */
    public static <T> ProgressExecutorTask<T> of(Executor executor, ProgressTask.Do<T> callback) {
        return new ProgressExecutorTask<T>(executor, callback);
    }
}
