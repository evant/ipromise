package me.tatarka.ipromise.task;

import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Promise;

import java.util.concurrent.Executor;

/**
 * A collection of helper methods for constructing tasks.
 *
 * @author Evan Tatarka
 */
public class Tasks {
    protected Tasks() {
    }

    /**
     * Runs the callback in a separate thread, delivering the messages to the returned {@link
     * me.tatarka.ipromise.Promise}. If the promise is canceled, the thread is interrupted. This is
     * equivalent to: {@code Task.of(callback).start()}.
     *
     * @param callback the callback to run in a separate thread
     * @param <T>      the result type
     * @return the promise that will receive the result
     */
    public static <T> Promise<T> run(Task.Do<T> callback) {
        return Tasks.of(callback).start();
    }

    /**
     * Runs the callback with the given {@link java.util.concurrent.Executor}, delivering the
     * messages to the returned {@link me.tatarka.ipromise.Promise}. This is equivalent to: {@code
     * Task.of(executor, callback).start()}.
     *
     * @param executor the executor used to run the callback
     * @param callback the callback
     * @param <T>      the result type
     * @return the promise that will receive the messages
     */
    public static <T> Promise<T> run(Executor executor, Task.Do<T> callback) {
        return Tasks.of(executor, callback).start();
    }

    /**
     * Runs the callback in a separate thread, delivering the messages to the returned {@link
     * me.tatarka.ipromise.Promise}. If the promise is canceled, the thread is interrupted. This is
     * equivalent to: {@code Task.of(deferredBuilder, callback).start()}.
     *
     * @param deferredBuilder the deferred builder
     * @param callback        teh callback
     * @param <T>             the message type
     * @return the promise that will receive the messages
     */
    public static <T> Promise<T> run(Deferred.Builder deferredBuilder, Task.Do<T> callback) {
        return Tasks.of(deferredBuilder, callback).start();
    }

    /**
     * Runs the callback with the given {@link java.util.concurrent.Executor}, delivering the
     * messages to the returned {@link me.tatarka.ipromise.Promise}. This is equivalent to: {@code
     * Task.of(deferredBuilder, executor, callback).start()}.
     *
     * @param deferredBuilder the deferred builder
     * @param executor        the executor used to run the callback
     * @param callback        the callback
     * @param <T>             the result type
     * @return the promise that will receive the messages
     */
    public static <T> Promise<T> run(Deferred.Builder deferredBuilder, Executor executor, Task.Do<T> callback) {
        return Tasks.of(deferredBuilder, executor, callback).start();
    }

    /**
     * Constructs a new {@link ExecutorTask} that calls the given callback in a separate thread when
     * {@link Task#start()} is called. If the {@link me.tatarka.ipromise.Promise} is canceled, the
     * thread is interrupted.
     *
     * @param callback the callback to run in a separate thread
     * @param <T>      the result type
     * @return the task
     * @see ExecutorTask
     */
    public static <T> Task<T> of(Task.Do<T> callback) {
        return new ThreadTask<T>(callback);
    }

    /**
     * Constructs a new {@link ExecutorTask} that calls the given callback in a separate thread when
     * {@link Task#start()} is called. If the {@link me.tatarka.ipromise.Promise} is canceled, the
     * thread is interrupted.
     *
     * @param deferredBuilder the deferred builder
     * @param callback        the callback to run in a separate thread
     * @param <T>             the result type
     * @return the task
     * @see ExecutorTask
     */
    public static <T> Task<T> of(Deferred.Builder deferredBuilder, Task.Do<T> callback) {
        return new ThreadTask<T>(deferredBuilder, callback);
    }

    /**
     * Constructs a new {@link ExecutorTask} that calls the given callback using the given {@link
     * java.util.concurrent.Executor} when {@link Task#start()} is called.
     *
     * @param executor the executor used to run the callback
     * @param callback the callback
     * @param <T>      the result type
     * @return the task
     * @see ExecutorTask
     */
    public static <T> Task<T> of(Executor executor, Task.Do<T> callback) {
        return new ExecutorTask<T>(executor, callback);
    }

    /**
     * Constructs a new {@link ExecutorTask} that calls the given callback using the given {@link
     * java.util.concurrent.Executor} when {@link Task#start()} is called.
     *
     * @param deferredBuilder the deferred builder
     * @param executor        the executor used to run the callback
     * @param callback        the callback
     * @param <T>             the result type
     * @return the task
     * @see ExecutorTask
     */
    public static <T> Task<T> of(Deferred.Builder deferredBuilder, Executor executor,
                                 Task.Do<T> callback) {
        return new ExecutorTask<T>(deferredBuilder, executor, callback);
    }
}
