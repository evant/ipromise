package me.tatarka.ipromise.task;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import me.tatarka.ipromise.Promise;

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
    public static <T> Promise<T> run(Task.Do<T> callback) {
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
    public static <T> Promise<T> run(Executor executor, Task.Do<T> callback) {
        return Tasks.of(executor, callback).start();
    }

    /**
     * Constructs a new {@link ExecutorTask} that calls the given callback in a separate
     * thread when {@link Task#start()} is called.
     *
     * @param callback the callback to run in a separate thread
     * @param <T>      the result type
     * @return the task
     * @see ExecutorTask
     */
    public static <T> ExecutorTask<T> of(Task.Do<T> callback) {
        return Tasks.of(Executors.newSingleThreadExecutor(), callback);
    }

    /**
     * Constructs a new {@link ExecutorTask} that calls the given callback using the given
     * {@link java.util.concurrent.Executor} when {@link Task#start()} is called.
     *
     * @param executor the executor used to run the callback
     * @param callback the callback
     * @param <T>      the result type
     * @return the task
     * @see ExecutorTask
     */
    public static <T> ExecutorTask<T> of(Executor executor, Task.Do<T> callback) {
        return new ExecutorTask<T>(executor, callback);
    }
}
