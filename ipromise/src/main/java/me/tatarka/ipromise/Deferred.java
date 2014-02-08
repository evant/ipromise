package me.tatarka.ipromise;

import me.tatarka.ipromise.buffer.PromiseBufferFactory;
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
        promise = new Promise<T>();
    }

    /**
     * Constructs a new {@code Deferred} with the given {@link me.tatarka.ipromise.CancelToken}.
     * When the cancel token is canceled, this deferred's {@link me.tatarka.ipromise.Promise} is
     * also canceled.
     *
     * @param cancelToken the cancel token
     */
    public Deferred(CancelToken cancelToken) {
        promise = new Promise<T>(cancelToken);
    }

    public Deferred(PromiseBufferFactory bufferFactor) {
        promise = new Promise<T>(bufferFactor);
    }

    public Deferred(int bufferType) {
        promise = new Promise<T>(PromiseBuffers.ofType(bufferType));
    }

    public Deferred(int bufferType, CancelToken cancelToken) {
        promise = new Promise<T>(PromiseBuffers.ofType(bufferType));
    }

    public Deferred(PromiseBufferFactory bufferFactory, CancelToken cancelToken) {
        promise = new Promise<T>(bufferFactory, cancelToken);
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
    public synchronized void resolve(T result) {
        if (promise == null) throw new Promise.AlreadyClosedException(result);
        promise.send(result);
        promise.close();
    }

    public synchronized void close() {
        promise.close();
        promise = null;
    }
}
