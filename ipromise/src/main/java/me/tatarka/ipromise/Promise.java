package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import me.tatarka.ipromise.func.Chain;
import me.tatarka.ipromise.func.Filter;
import me.tatarka.ipromise.func.Map;

/**
 * <p> A promise is a way to return a result the will be fulfilled sometime in the future. This
 * fixes the inversion of control that callback-style functions creates and restores the
 * composeability of return values. </p>
 *
 * <p> In addition to creating a standard interface for all asynchronous functions, Promises can
 * also more-robustly handle error and cancellation situations. </p>
 *
 * <p> You cannot construct a {@code Promise} directly, instead you must get one from a {@link
 * Deferred}. That is, unless the result is already available. </p>
 *
 * @author Evan Tatarka
 * @see Deferred
 */
public class Promise<T> {
    public static final int BUFFER_NONE = 0;
    public static final int BUFFER_LAST = 1;
    public static final int BUFFER_ALL = 2;

    private CancelToken cancelToken;
    private boolean isClosed;

    protected final Executor callbackExecutor;
    protected final List<Listener<T>> listeners = new ArrayList<Listener<T>>();
    protected final List<CloseListener> closeListeners = new ArrayList<CloseListener>();

    protected Promise() {
        this(new CancelToken(), CallbackExecutors.getDefault());
    }

    protected Promise(CancelToken cancelToken, Executor callbackExecutor) {
        this.cancelToken = cancelToken;
        this.callbackExecutor = callbackExecutor;
    }

    protected Promise(final Promise parentPromise) {
        this(parentPromise.cancelToken, parentPromise.callbackExecutor);
        parentPromise.onClose(new CloseListener() {
            @Override
            public void close() {
                Promise.this.close();
            }
        });
    }

    /**
     * Returns the promise's {@link me.tatarka.ipromise.CancelToken}.
     *
     * @return the cancel token
     */
    public CancelToken cancelToken() {
        return cancelToken;
    }

    /**
     * Delivers a message to all listeners of the {@code Promise}. This is used internally by {@link
     * Deferred}.
     *
     * @param message the message to send
     * @throws AlreadyClosedException thrown if the {@code Promise} has already been closed
     */
    synchronized void send(T message) {
        if (cancelToken.isCanceled()) return;
        if (isClosed) throw new AlreadyClosedException(message);

        onSend(message);

        for (Listener<T> listener : listeners) {
            dispatch(callbackExecutor, listener, message);
        }
    }

    static <T> void dispatch(Executor callbackExecutor, final Listener<T> listener, final T message) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                listener.receive(message);
            }
        });
    }

    static void dispatchClose(Executor callbackExecutor, final CloseListener listener) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                listener.close();
            }
        });
    }

    protected void onSend(T message) {
    }

    /**
     * Notifies the {@code Promise} that no more messages will be sent. This is used internally by
     * {@link Deferred}.
     */
    synchronized void close() {
        isClosed = true;
        listeners.clear();

        for (CloseListener listener : closeListeners) {
            dispatchClose(callbackExecutor, listener);
        }
        closeListeners.clear();
    }

    /**
     * Cancels the {@code Promise}, notifying all listeners and propagating the cancellation to all
     * Promises that share the {@link CancelToken}.
     */
    public synchronized void cancel() {
        cancelToken.cancel();
    }

    /**
     * Returns if the {@code Promise} has been closed.
     *
     * @return true if closed, false otherwise
     */
    public synchronized boolean isClosed() {
        return isClosed;
    }

    /**
     * Returns if the promise has been canceled.
     *
     * @return true if canceled, false otherwise
     */
    public synchronized boolean isCanceled() {
        return cancelToken.isCanceled();
    }

    /**
     * Returns if the promise is running, (i.e. that is has not been closed or canceled).
     *
     * @return true if running, false otherwise
     */
    public synchronized boolean isRunning() {
        return !isClosed && !isCanceled();
    }

    /**
     * Listens to a {@code Promise}, getting a result whenever a message is sent. If and how many
     * previous results are returned depends on the {@link me.tatarka.ipromise.buffer.PromiseBuffer}.
     *
     * @param listener the listener to call when the promise receives a message
     * @return the {@code Promise} for chaining
     */
    public synchronized Promise<T> listen(final Listener<T> listener) {
        if (listener == null) return this;

        onListen(listener);

        if (!isClosed()) listeners.add(listener);

        return this;
    }

    protected void onListen(Listener<T> listener) {
    }

    /**
     * Listens to a {@code Promise}, receiving a callback when it is closed, i.e. it wont receive
     * any more messages. If the {@code Promise} is already closed, the callback will be called
     * immediately.
     *
     * @param listener the listener
     * @return the {@code Promise} for chaining
     */
    public synchronized Promise<T> onClose(CloseListener listener) {
        if (listener == null) return this;

        if (isClosed) {
            dispatchClose(callbackExecutor, listener);
        } else {
            closeListeners.add(listener);
        }

        return this;
    }

    /**
     * Constructs a new {@code Promise} that returns when the original promise returns but passes
     * the result through the given {@link me.tatarka.ipromise.func.Map} function.
     *
     * @param map  the function to chain the result of the original {@code Promise} to the new
     *             promise
     * @param <T2> the result type of the new {@code Promise}
     * @return the new {@code Promise}
     */
    public synchronized <T2> Promise<T2> then(final Map<T, T2> map) {
        final Promise<T2> newPromise = new Promise<T2>(this);
        listen(new Listener<T>() {
            @Override
            public void receive(T message) {
                newPromise.send(map.map(message));
            }
        });
        return newPromise;
    }

    /**
     * Constructs a new {@code Promise} that chains two promises in succession.
     *
     * @param chain the {@link me.tatarka.ipromise.func.Chain} that constructs the second {@code
     *              Promise}
     * @param <T2>  the type of the second {@code Promise} result
     * @return the new {@code Promise}
     */
    public synchronized <T2> Promise<T2> then(final Chain<T, Promise<T2>> chain) {
        final Promise<T2> newPromise = new Promise<T2>(this);
        listen(new Listener<T>() {
            @Override
            public void receive(T message) {
                chain.chain(message).listen(new Listener<T2>() {
                    @Override
                    public void receive(T2 message) {
                        newPromise.send(message);
                    }
                });
            }
        });
        return newPromise;
    }

    /**
     * Constructs a new {@code Promise} that filters this {@code Promise}. i.e. the new {@code
     * Promise} will not receive any messages when {@link me.tatarka.ipromise.func.Filter#filter(Object)}
     * returns false.
     *
     * @param filter the filter
     * @return the new {@code Progress}
     */
    public synchronized Promise<T> then(final Filter<T> filter) {
        final Promise<T> newPromise = new Promise<T>(this);
        listen(new Listener<T>() {
            @Override
            public void receive(T message) {
                if (filter.filter(message)) {
                    newPromise.send(message);
                }
            }
        });
        return newPromise;
    }

    /**
     * Constructs a new {@code Promise} that batches the messages of this {@code Promise}.
     *
     * @param size the batch's size. The new {@code Promise} will be called every time this many
     *             messages are sent, and again with the remaining messages when the {@code Promise}
     *             is closed.
     * @return the new {@code Promise}
     */
    public synchronized Promise<List<T>> batch(final int size) {
        final Promise<List<T>> newPromise = new Promise<List<T>>(cancelToken, callbackExecutor);
        final List<T> batchedItems = new ArrayList<T>();
        listen(new Listener<T>() {
            @Override
            public void receive(T message) {
                batchedItems.add(message);
                if (batchedItems.size() >= size) {
                    newPromise.send(new ArrayList<T>(batchedItems));
                    batchedItems.clear();
                }
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                if (!batchedItems.isEmpty()) {
                    newPromise.send(new ArrayList<T>(batchedItems));
                    batchedItems.clear();
                }
                newPromise.close();
            }
        });
        return newPromise;
    }

    /**
     * Constructs a new {@code Promise} that receives a message after both of the given promises
     * receive a message. This is a type-safe version of {@link Promise#and(Promise[])} for two
     * promises.
     *
     * @param promise the other {@code Promise}
     * @param <T2>    the type of the other {@code Promise}
     * @return the new {@code Promise}
     * @see Promise#and(Promise[])
     */
    public <T2> Promise<Pair<T, T2>> and(Promise<T2> promise) {
        return and(this, promise).then(new Map<Object[], Pair<T, T2>>() {
            @Override
            public Pair<T, T2> map(Object[] result) {
                return Pair.of((T) result[0], (T2) result[1]);
            }
        });
    }

    /**
     * Constructs a new {@code Promise} that waits before all given promises receive a message
     * before sending them to the new {@code Promise}, minus any promises that have already been
     * closed. The resulting array will contain the messages at the same indices as the
     * corresponding promises. If the promise has been closed, the corresponding message will be
     * null. The new {@code Promise} is closed, when all of the given promises are closed.
     *
     * @param promises the promises
     * @return the new {@code Promise}
     */
    public static Promise<Object[]> and(final Promise... promises) {
        if (promises == null) throw new NullPointerException();

        final Promise<Object[]> newPromise = new Promise<Object[]>();
        final AtomicInteger count = new AtomicInteger();
        final AtomicInteger size = new AtomicInteger(promises.length);
        final Object[] results = new Object[promises.length];

        final Object lock = new Object();

        for (int i = 0; i < promises.length; i++) {
            final int index = i;
            CancelToken.join(newPromise.cancelToken, promises[index].cancelToken);
            promises[index].listen(new Listener() {
                @Override
                public void receive(Object message) {
                    synchronized (lock) {
                        results[index] = message;
                        int done = count.incrementAndGet();

                        if (done >= size.get()) {
                            count.set(0);
                            newPromise.send(Arrays.copyOf(results, results.length));
                            for (int i = 0; i < results.length; i++) results[i] = null;
                        }
                    }
                }
            }).onClose(new CloseListener() {
                @Override
                public void close() {
                    if (size.decrementAndGet() == 0) {
                        newPromise.close();
                    }
                }
            });
        }

        return newPromise;
    }

    /**
     * Constructs a new {@code Promise} that receives a message when either of the given promises
     * receive a message. This is a type-safe version of {@link Promise#merge(Promise[])} for two
     * promises.
     *
     * @param promise the other {@code Promise}.
     * @return the new {@code Promise}
     * @see Promise#merge(Promise[])
     */
    public Promise<T> merge(final Promise<? extends T> promise) {
        return merge(this, promise).cast();
    }

    /**
     * Constructs a new {@code Promise} that receives a message when any of the given promises
     * receive a message.
     *
     * @param promises the promises
     * @return the new {@code Promise}
     */
    public static Promise<Object> merge(final Promise... promises) {
        if (promises == null) throw new NullPointerException();

        final Promise<Object> newPromise = new Promise<Object>();
        final AtomicInteger canceledCount = new AtomicInteger();
        final AtomicInteger size = new AtomicInteger(promises.length);

        newPromise.cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                for (Promise promise : promises) promise.cancel();
            }
        });

        for (Promise promise : promises) {
            // We can't just join cancel tokens here because the new promise should only cancel if
            // all of the given promises are canceled.
            promise.cancelToken.listen(new CancelToken.Listener() {
                @Override
                public void canceled() {
                    int count = canceledCount.incrementAndGet();
                    if (count >= size.get()) newPromise.cancel();
                }
            });

            promise.listen(new Listener() {
                @Override
                public void receive(Object message) {
                    newPromise.send(message);
                }
            }).onClose(new CloseListener() {
                @Override
                public void close() {
                    if (size.decrementAndGet() == 0) {
                        newPromise.close();
                    }
                }
            });
        }

        return newPromise;
    }

    /**
     * Forces a cast of a promise to a super-type to get around covariance restrictions. {@code T2}
     * must be a superclass of {@code T}. This is safe because you can't directly deliver results to
     * the cast promise.
     *
     * @param <T2> the type of the result to cast to
     * @return a cast of the promise
     */
    @SuppressWarnings("unchecked")
    public <T2> Promise<T2> cast() {
        return (Promise<T2>) this;
    }

    /**
     * The Exception thrown if a result has already been delivered and a second result is attempted
     * to be delivered.
     */
    public static class AlreadyClosedException extends IllegalStateException {
        public AlreadyClosedException(Object message) {
            super(message + " cannot be sent because the promise has already been closed.");
        }
    }
}
