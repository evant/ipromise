package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A promise is a way to return a result the will be fulfilled sometime in the future. This fixes
 * the inversion of control that callback-style functions creates and restores the composeability of
 * return values.
 * <p/>
 * In addition to creating a standard interface for all asynchronous functions, Promises can also
 * more-robustly handle error and cancellation situations.
 * <p/>
 * You cannot construct a {@code Promise} directly, instead you must get one from a {@link
 * Deferred}. That is, unless the result is already available.
 *
 * @author Evan Tatarka
 * @see Deferred
 */
public class Promise<T> {
    public static final int BUFFER_NONE = 0;
    public static final int BUFFER_LAST = 1;
    public static final int BUFFER_ALL = 2;

    private CancelToken cancelToken;
    private List<Listener<T>> listeners = new ArrayList<Listener<T>>();
    private List<CloseListener> closeListeners = new ArrayList<CloseListener>();
    private PromiseBufferFactory bufferFactory;
    private PromiseBuffer<T> buffer;
    private boolean isClosed;

    /**
     * Constructs a new promise. This is used internally by {@link Deferred}.
     */
    public Promise() {
        this(new CancelToken());
    }

    /**
     * Constructs a new promise with the given {@link CancelToken}. When the token is canceled, the
     * promise is also canceled. This is used internally by {@link Deferred}.
     *
     * @param cancelToken the cancel token
     */
    Promise(CancelToken cancelToken) {
        this(PromiseBuffers.last(), cancelToken);
    }

    Promise(PromiseBufferFactory bufferFactory) {
        this(bufferFactory, new CancelToken());
    }

    Promise(PromiseBufferFactory bufferFactory, CancelToken cancelToken) {
        this.bufferFactory = bufferFactory;
        this.buffer = bufferFactory.create();
        this.cancelToken = cancelToken;
        cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                listeners.clear();
            }
        });
    }

    /**
     * Constructs a promise with a result already in it. This is useful for when you can return the
     * value immediately.
     *
     * @param result the result
     */
    public Promise(T result) {
        cancelToken = new CancelToken();
        buffer = new ArrayPromiseBuffer<T>(1);
        buffer.add(result);
        isClosed = true;
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
     */
    synchronized void send(T message) {
        if (cancelToken.isCanceled()) return;
        if (isClosed) throw new AlreadyClosedException(message);
        buffer.add(message);

        for (Listener<T> listener : listeners) {
            listener.receive(message);
        }
    }

    /**
     * Notifies the {@code Promise} that no more messages will be sent. This is used internally by
     * {@link Deferred}.
     */
    synchronized void close() {
        isClosed = true;
        listeners.clear();

        for (CloseListener listener : closeListeners) {
            listener.close();
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
     * previous results are returned depends on the {@link me.tatarka.ipromise.PromiseBuffer}.
     *
     * @param listener the listener to call when the promise receives a message
     * @return the {@code Promise} for chaining
     */
    public synchronized Promise<T> listen(Listener<T> listener) {
        if (listener == null) return this;

        for (T message : buffer) {
            listener.receive(message);
        }

        if (!isClosed) {
            listeners.add(listener);
        }

        return this;
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
            listener.close();
        } else {
            closeListeners.add(listener);
        }

        return this;
    }

    /**
     * Constructs a new {@code Promise} that returns when the original promise returns but passes
     * the result through the given {@link Map} function.
     *
     * @param map  the function to chain the result of the original {@code Promise} to the new
     *             promise
     * @param <T2> the result type of the new {@code Promise}
     * @return the new {@code Promise}
     */
    public <T2> Promise<T2> then(final Map<T, T2> map) {
        final Promise<T2> newPromise = new Promise<T2>(bufferFactory, cancelToken);
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
                newPromise.send(map.map(result));
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                newPromise.close();
            }
        });
        return newPromise;
    }

    /**
     * Constructs a new {@code Promise} that chains two promises in succession.
     *
     * @param chain the {@link Chain} that constructs the second {@code Promise}
     * @param <T2>  the type of the second {@code Promise} result
     * @return the new {@code Promise}
     */
    public <T2> Promise<T2> then(final Chain<T, Promise<T2>> chain) {
        final Promise<T2> newPromise = new Promise<T2>(bufferFactory, cancelToken);
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
                final Promise<T2> chainedPromise = chain.chain(result);
                CancelToken.join(cancelToken, chainedPromise.cancelToken);
                chainedPromise.listen(new Listener<T2>() {
                    @Override
                    public void receive(T2 result) {
                        newPromise.send(result);
                    }
                });
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                newPromise.close();
            }
        });
        return newPromise;
    }

    /**
     * Constructs a new {@code Promise} that filters this {@code Promise}. i.e. the new {@code
     * Promise} will not receive any messages when {@link me.tatarka.ipromise.Filter#filter(Object)}
     * returns false.
     *
     * @param filter the filter
     * @return the new {@code Progress}
     */
    public synchronized Promise<T> then(final Filter<T> filter) {
        final Promise<T> newProgress = new Promise<T>(bufferFactory, cancelToken);
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
                if (filter.filter(result)) newProgress.send(result);
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                newProgress.close();
            }
        });
        return newProgress;
    }

    /**
     * Constructs a new {@code Promise} that batches the messages of this {@code Promise}.
     *
     * @param size the batch's size. The new {@code Promise} will be called every time this many
     *             messages are sent, and again with the remaining messages when the {@code
     *             Promise} is closed.
     * @return the new {@code Promise}
     */
    public synchronized Promise<List<T>> batch(final int size) {
        final Promise<List<T>> newProgress = new Promise<List<T>>(bufferFactory, cancelToken);
        final List<T> batchedItems = new ArrayList<T>();
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
                batchedItems.add(result);
                if (batchedItems.size() >= size) {
                    newProgress.send(new ArrayList<T>(batchedItems));
                    batchedItems.clear();
                }
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                if (!batchedItems.isEmpty()) {
                    newProgress.send(new ArrayList<T>(batchedItems));
                    batchedItems.clear();
                }
                newProgress.close();
            }
        });
        return newProgress;
    }

    /**
     * Constructs a new {@code Promise} that completes after both the current and given promises
     * complete.
     *
     * @param promise the other {@code Promise}
     * @param <T2>    the type of the other {@code Promise}
     * @return the new {@code Promise}
     */
    public <T2> Promise<Pair<T, T2>> and(Promise<T2> promise) {
        return and(bufferFactory, this, promise).then(new Map<List, Pair<T, T2>>() {
            @Override
            public Pair<T, T2> map(List result) {
                return Pair.of((T) result.get(0), (T2) result.get(1));
            }
        });
    }

    /**
     * Constructs a new {@code Promise} that waits before all given promises have a result before
     * sending them to the new {@code Promise}, minus any promises that have already been closed.
     * The new {@code Promise} is closed, when all of the given promises are closed.
     *
     * @param promises the promises
     * @return the new {@code Promise}
     */
    public static Promise<List> and(PromiseBufferFactory bufferFactory, final Promise... promises) {
        if (promises == null) throw new NullPointerException();

        final Promise<List> newPromise = new Promise<List>(bufferFactory);
        final AtomicInteger count = new AtomicInteger();
        final AtomicInteger size = new AtomicInteger(promises.length);
        final List results = new ArrayList(promises.length);
        for (Promise _ : promises) results.add(null);

        final Object lock = new Object();

        for (int i = 0; i < promises.length; i++) {
            final int index = i;
            CancelToken.join(newPromise.cancelToken, promises[index].cancelToken);
            promises[index].listen(new Listener() {
                @Override
                public void receive(Object result) {
                    synchronized (lock) {
                        results.set(index, result);
                        int done = count.incrementAndGet();

                        if (done >= size.get()) {
                            count.set(0);
                            newPromise.send(results);
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
     * receive a message.
     *
     * @param promise the other {@code Promise}.
     * @return the new {@code Promise}
     */
    public Promise<T> merge(final Promise<T> promise) {
        return merge(this, promise);
    }

    /**
     * Constructs a new {@code Promise} that receives a message when any of the given promises
     * receive a message.
     *
     * @param promises the promises
     * @param <T>      the type of the promises
     * @return the new {@code Promise}
     */
    public static <T> Promise<T> merge(final Promise<T>... promises) {
        if (promises == null) throw new NullPointerException();

        final Promise<T> newPromise = new Promise<T>();
        final AtomicInteger canceledCount = new AtomicInteger();
        final AtomicInteger size = new AtomicInteger(promises.length);

        newPromise.cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                for (Promise<T> promise : promises) promise.cancel();
            }
        });

        for (Promise<T> promise : promises) {
            // We can't just join cancel tokens here because the new promise should only cancel if
            // all of the given promises are canceled.
            promise.cancelToken.listen(new CancelToken.Listener() {
                @Override
                public void canceled() {
                    int count = canceledCount.incrementAndGet();
                    if (count >= size.get()) newPromise.cancel();
                }
            });

            promise.listen(new Listener<T>() {
                @Override
                public void receive(T result) {
                    newPromise.send(result);
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
