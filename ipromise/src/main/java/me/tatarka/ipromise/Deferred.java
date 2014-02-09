package me.tatarka.ipromise;

import java.util.Arrays;
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

    /**
     * Constructs a new {@code Deferred} with the given {@link me.tatarka.ipromise.CancelToken}.
     * When the token is canceled, this deferred's {@link me.tatarka.ipromise.Promise} is also
     * canceled.
     *
     * @param cancelToken the cancel token
     */
    public Deferred(CancelToken cancelToken) {
        this(PromiseBuffers.<T>last(), cancelToken, Promise.getDefaultCallbackExecutor());
    }

    /**
     * Constructs a new {@code Deferred} with the given {@link me.tatarka.ipromise.buffer.PromiseBuffer}.
     * The deferred's {@link me.tatarka.ipromise.Promise} will use the given buffer to buffer
     * messages and redeliver them when a {@link me.tatarka.ipromise.Listener} is attached.
     *
     * @param buffer the promise buffer
     */
    public Deferred(PromiseBuffer<T> buffer) {
        this(buffer, new CancelToken(), Promise.getDefaultCallbackExecutor());
    }

    /**
     * Constructs a new {@code Deferred} with the given {@link me.tatarka.ipromise.buffer.PromiseBuffer}
     * and {@link me.tatarka.ipromise.CancelToken}. The deferred's {@link
     * me.tatarka.ipromise.Promise} will use the given buffer to buffer messages and redeliver them
     * when a {@link me.tatarka.ipromise.Listener} is attached. When the token is canceled, this
     * deferred's {@code Promise} is also canceled.
     *
     * @param buffer      the promise buffer
     * @param cancelToken the cancel token
     */
    public Deferred(PromiseBuffer<T> buffer, CancelToken cancelToken) {
        this(buffer, cancelToken, Promise.getDefaultCallbackExecutor());
    }

    /**
     * Constructs a new {@code Deferred} with the given {@link me.tatarka.ipromise.buffer.PromiseBuffer}
     * type. Valid values are {@link Promise#BUFFER_NONE}, {@link Promise#BUFFER_LAST}, and {@link
     * Promise#BUFFER_ALL}. The deferred's {@link me.tatarka.ipromise.Promise} will use the given
     * buffer to buffer messages and redeliver them when a {@link me.tatarka.ipromise.Listener} is
     * attached.
     *
     * @param bufferType the promise buffer
     */
    public Deferred(int bufferType) {
        this(PromiseBuffers.<T>ofType(bufferType), new CancelToken(), Promise.getDefaultCallbackExecutor());
    }

    /**
     * Constructs a new {@code Deferred} with the given {@link me.tatarka.ipromise.buffer.PromiseBuffer}
     * type and {@link me.tatarka.ipromise.CancelToken}. Valid values for the buffer are {@link
     * Promise#BUFFER_NONE}, {@link Promise#BUFFER_LAST}, and {@link Promise#BUFFER_ALL}. The
     * deferred's {@link me.tatarka.ipromise.Promise} will use the given buffer to buffer messages
     * and redeliver them when a {@link me.tatarka.ipromise.Listener} is attached. When the token is
     * canceled, this deferred's {@code Promise} is also canceled.
     *
     * @param bufferType  the promise buffer
     * @param cancelToken the cancel token
     */
    public Deferred(int bufferType, CancelToken cancelToken) {
        this(PromiseBuffers.<T>ofType(bufferType), cancelToken, Promise.getDefaultCallbackExecutor());
    }

    /**
     * Constructs a new {@code Deferred} with the given {@link me.tatarka.ipromise.buffer.PromiseBuffer},
     * {@link me.tatarka.ipromise.CancelToken}, and callback executor. The deferred's {@link
     * me.tatarka.ipromise.Promise} will use the given buffer to buffer messages and redeliver them
     * when a {@link me.tatarka.ipromise.Listener} is attached. When the token is canceled, this
     * deferred's {@code Promise} is also canceled. The callback executor is used to run the {@link
     * Promise#listen(Listener)}  and {@link Promise#onClose(CloseListener)} callbacks. The default
     * is to run callbacks on a single background thread. See {@link Promise#setDefaultCallbackExecutor(java.util.concurrent.Executor)}
     * for more details.
     *
     * @param buffer      the promise buffer
     * @param cancelToken the cancel token
     * @see Promise#setDefaultCallbackExecutor(java.util.concurrent.Executor)
     */
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

    /**
     * Sends a message to the {@link me.tatarka.ipromise.Promise}.
     *
     * @param message the message to send
     * @return the {@code Deferred} for chaining
     * @throws me.tatarka.ipromise.Promise.AlreadyClosedException thrown if the promise as already
     *                                                            been closed
     */
    public synchronized Deferred<T> send(T message) {
        promise.send(message);
        return this;
    }

    /**
     * Sends all of the messages to the {@link me.tatarka.ipromise.Promise}.
     *
     * @param messages the messages to send
     * @return the {@code Deferred} for chaining
     * @throws me.tatarka.ipromise.Promise.AlreadyClosedException thrown if the promise as already
     *                                                            been closed
     */
    public synchronized Deferred<T> sendAll(Iterable<T> messages) {
        for (T message : messages) promise.send(message);
        return this;
    }

    /**
     * Sends all of the messages to the {@link me.tatarka.ipromise.Promise}.
     *
     * @param messages the messages to send
     * @return the {@code Deferred} for chaining
     * @throws me.tatarka.ipromise.Promise.AlreadyClosedException thrown if the promise as already
     *                                                            been closed
     */
    public synchronized Deferred<T> sendAll(T... messages) {
        return sendAll(Arrays.asList(messages));
    }

    /**
     * Sends a message to the {@link me.tatarka.ipromise.Promise} and immediately closes it. This is
     * useful if you only have one message to send.
     *
     * @param message the message to send
     * @throws me.tatarka.ipromise.Promise.AlreadyClosedException thrown if the promise as already
     *                                                            been closed
     */
    public synchronized Deferred<T> resolve(T message) {
        promise.send(message);
        close();
        return this;
    }

    /**
     * Sends all of the messages to the {@link me.tatarka.ipromise.Promise} and immediately closes
     * it. This is useful if you only have no more messages to send.
     *
     * @param messages the messages to send
     * @throws me.tatarka.ipromise.Promise.AlreadyClosedException thrown if the promise as already
     *                                                            been closed
     */
    public synchronized Deferred<T> resolveAll(Iterable<T> messages) {
        for (T message : messages) promise.send(message);
        close();
        return this;
    }

    /**
     * Sends all of the messages to the {@link me.tatarka.ipromise.Promise} and immediately closes
     * it. This is useful if you only have no more messages to send.
     *
     * @param messages the messages to send
     * @throws me.tatarka.ipromise.Promise.AlreadyClosedException thrown if the promise as already
     *                                                            been closed
     */
    public synchronized Deferred<T> resolveAll(T... messages) {
        return resolveAll(Arrays.asList(messages));
    }

    /**
     * Closes the {@link me.tatarka.ipromise.Promise}. After this, no more messages can be sent.
     * This must be called when you don't have any more message to send.
     */
    public synchronized void close() {
        promise.close();
    }

    /**
     * A builder to configure and construct multiple deferreds. The {@code Builder} is immutable and
     * all methods return a new instance. For now the callback executor is the only thing that you
     * can configure.
     *
     * @see Promise#setDefaultCallbackExecutor(java.util.concurrent.Executor)
     */
    public static class Builder {
        private Executor callbackExecutor;

        /**
         * Constructs a new {@code Builder}
         */
        public Builder() {
        }

        /**
         * Constructs a new {@code Builder} that inherits from the given {@code Builder}.
         *
         * @param builder the builder
         */
        public Builder(Builder builder) {
            callbackExecutor = builder.callbackExecutor;
        }

        /**
         * Constructs a new {@code Builder} with the given callback executor. This is a convenience
         * method for {@code new Deferred.Builder().callbackExecutor(executor)}.
         *
         * @param callbackExecutor the callback executor
         * @return the builder
         * @see Promise#setDefaultCallbackExecutor(java.util.concurrent.Executor)
         */
        public static Builder withCallbackExecutor(Executor callbackExecutor) {
            return new Builder().callbackExecutor(callbackExecutor);
        }

        /**
         * Sets the callback executor.
         *
         * @param callbackExecutor the callback executor
         * @return the new builder
         * @see Promise#setDefaultCallbackExecutor(java.util.concurrent.Executor)
         */
        public Builder callbackExecutor(Executor callbackExecutor) {
            Builder builder = new Builder(this);
            builder.callbackExecutor = callbackExecutor;
            return builder;
        }

        /**
         * Builds a new {@link me.tatarka.ipromise.Deferred}.
         *
         * @param <T> the message type
         * @return the new {@code Deferred}
         * @see Deferred#Deferred()
         */
        public <T> Deferred<T> build() {
            return build(PromiseBuffers.<T>last());
        }

        /**
         * Builds a new {@link me.tatarka.ipromise.Deferred}.
         *
         * @param <T>    the message type
         * @param buffer the promise buffer
         * @return the new {@code Deferred}
         * @see Deferred#Deferred(me.tatarka.ipromise.buffer.PromiseBuffer)
         */
        public <T> Deferred<T> build(PromiseBuffer<T> buffer) {
            return build(buffer, new CancelToken());
        }

        /**
         * Builds a new {@link me.tatarka.ipromise.Deferred}.
         *
         * @param <T>         the message type
         * @param cancelToken the cancel token
         * @return the new {@code Deferred}
         * @see Deferred#Deferred(me.tatarka.ipromise.CancelToken)
         */
        public <T> Deferred<T> build(CancelToken cancelToken) {
            return build(PromiseBuffers.<T>last(), cancelToken);
        }

        /**
         * Builds a new {@link me.tatarka.ipromise.Deferred}.
         *
         * @param <T>        the message type
         * @param bufferType the buffer type
         * @return the new {@code Deferred}
         * @see Deferred#Deferred(int)
         */
        public <T> Deferred<T> build(int bufferType) {
            return build(PromiseBuffers.<T>ofType(bufferType));
        }

        /**
         * Builds a new {@link me.tatarka.ipromise.Deferred}.
         *
         * @param <T>         the message type
         * @param bufferType  the buffer type
         * @param cancelToken the cancel token
         * @return the new {@code Deferred}
         * @see Deferred#Deferred(int, me.tatarka.ipromise.CancelToken)
         */
        public <T> Deferred<T> build(int bufferType, CancelToken cancelToken) {
            return build(PromiseBuffers.<T>ofType(bufferType), cancelToken);
        }

        /**
         * Builds a new {@link me.tatarka.ipromise.Deferred}.
         *
         * @param <T>         the message type
         * @param buffer      the promise buffer
         * @param cancelToken the cancel token
         * @return the new {@code Deferred}
         * @see Deferred#Deferred(me.tatarka.ipromise.buffer.PromiseBuffer,
         * me.tatarka.ipromise.CancelToken)
         */
        public <T> Deferred<T> build(PromiseBuffer<T> buffer, CancelToken cancelToken) {
            if (callbackExecutor == null) callbackExecutor = Promise.getDefaultCallbackExecutor();
            return new Deferred<T>(buffer, cancelToken, callbackExecutor);
        }
    }
}
