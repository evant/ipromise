package me.tatarka.ipromise;

import java.util.concurrent.Executor;

import me.tatarka.ipromise.buffer.PromiseBuffer;
import me.tatarka.ipromise.buffer.PromiseBuffers;

/**
 * A {@code Deferred} is the producer end of a {@link Promise}. An asynchronous method creates a
 * {@code Deferred} and returns {@link Deferred#promise()}, then calls {@link
 * Deferred#resolve(Object)} at a later time.
 *
 * @param <T> the type of a result
 * @author Evan Tatarka
 */
public class Deferred<T> {
    private Promise<T> promise;

    /**
     * Constructs a new {@code Deferred}.
     */
    public Deferred() {
        this(PromiseBuffers.<T>last(), new CancelToken(), Promise.getDefaultCallbackExecutor());
    }

    public Deferred(CancelToken cancelToken) {
        this(PromiseBuffers.<T>last(), cancelToken, Promise.getDefaultCallbackExecutor());
    }

    public Deferred(PromiseBuffer<T> buffer) {
        this(buffer, new CancelToken(), Promise.getDefaultCallbackExecutor());
    }

    public Deferred(PromiseBuffer<T> buffer, CancelToken cancelToken) {
        this(buffer, cancelToken, Promise.getDefaultCallbackExecutor());
    }

    public Deferred(int bufferType) {
        this(PromiseBuffers.<T>ofType(bufferType), new CancelToken(), Promise.getDefaultCallbackExecutor());
    }

    public Deferred(int bufferType, CancelToken cancelToken) {
        this(PromiseBuffers.<T>ofType(bufferType), cancelToken, Promise.getDefaultCallbackExecutor());
    }

    public Deferred(PromiseBuffer<T> buffer, CancelToken cancelToken, Executor callbackExecutor) {
        promise = new ValuePromise<T>(buffer, cancelToken, callbackExecutor);
    }

    /**
     * The deferred's {@link me.tatarka.ipromise.Promise}.
     *
     * @return the promise
     */
    public Promise<T> promise() {
        return promise;
    }

    public synchronized Deferred<T> send(T result) {
        if (promise == null) throw new Promise.AlreadyClosedException(result);
        promise.send(result);
        return this;
    }

    /**
     * Delivers a result to all listeners of the {@code Promise}. Only one result can be delivered.
     * If the promise has already been canceled, the result will not be stored and listeners will
     * not be notified.
     *
     * @param result the result to reject.
     * @throws me.tatarka.ipromise.Promise.AlreadyClosedException throws if a result has already been delivered.
     */
    public synchronized Deferred<T> resolve(T result) {
        if (promise == null) throw new Promise.AlreadyClosedException(result);
        promise.send(result);
        promise.close();
        return this;
    }

    public synchronized void close() {
        promise.close();
        promise = null;
    }

    public static class Builder {
        private Executor callbackExecutor;

        public Builder() {
        }

        public Builder(Builder builder) {
            callbackExecutor = builder.callbackExecutor;
        }

        public static Builder withCallbackExecutor(Executor callbackExecutor) {
            return new Builder().callbackExecutor(callbackExecutor);
        }

        public Builder callbackExecutor(Executor callbackExecutor) {
            Builder builder = new Builder(this);
            builder.callbackExecutor = callbackExecutor;
            return builder;
        }

        public <T> Deferred<T> build() {
            return build(PromiseBuffers.<T>last());
        }

        public <T> Deferred<T> build(PromiseBuffer<T> buffer) {
            return build(buffer, new CancelToken());
        }

        public <T> Deferred<T> build(CancelToken cancelToken) {
            return build(PromiseBuffers.<T>last(), cancelToken);
        }

        public <T> Deferred<T> build(int bufferType) {
            return build(PromiseBuffers.<T>ofType(bufferType));
        }

        public <T> Deferred<T> build(int bufferType, CancelToken cancelToken) {
            return build(PromiseBuffers.<T>ofType(bufferType), cancelToken);
        }

        public <T> Deferred<T> build(PromiseBuffer<T> buffer, CancelToken cancelToken) {
            if (callbackExecutor == null) callbackExecutor = Promise.getDefaultCallbackExecutor();
            return new Deferred<T>(buffer, cancelToken, callbackExecutor);
        }

    }
}
