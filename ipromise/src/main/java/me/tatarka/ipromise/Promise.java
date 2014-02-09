package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import me.tatarka.ipromise.func.Chain;
import me.tatarka.ipromise.func.Filter;
import me.tatarka.ipromise.func.Map;

/**
 * A promise is a way to return a result the will be fulfilled sometime in the future. This fixes
 * the inversion of control that callback-style functions creates and restores the composeability of
 * return values. <p/> In addition to creating a standard interface for all asynchronous functions,
 * Promises can also more-robustly handle error and cancellation situations. <p/> You cannot
 * construct a {@code Promise} directly, instead you must get one from a {@link Deferred}. That is,
 * unless the result is already available.
 *
 * @author Evan Tatarka
 * @see Deferred
 */
public class Promise<T> {
    public static final int BUFFER_NONE = 0;
    public static final int BUFFER_LAST = 1;
    public static final int BUFFER_ALL = 2;

    private static Executor defaultCallbackExecutor;
    private static Executor sameThreadCallbackExecutor;

    private CancelToken cancelToken;
    private boolean isClosed;

    protected final Executor callbackExecutor;
    protected final List<Listener<T>> listeners = new ArrayList<Listener<T>>();
    protected final List<CloseListener> closeListeners = new ArrayList<CloseListener>();

    /**
     * <p> Sets the default callback executor used to run the {@link Promise#listen(Listener)} and
     * {@link Promise#onClose(CloseListener)} callbacks. The default is to run callbacks on a single
     * background thread. This is important to give consistency in what execution context the
     * callbacks are run in. It can also be uses to simplify you threading model, e.x. you could
     * post all callbacks to the UI thread. If you want more information on why this is a good idea,
     * see <a href="http://blog.ometer.com/2011/07/24/callbacks-synchronous-and-asynchronous/">this
     * blog post</a>. </p>
     *
     * <p> <b>Important:</b> The given executor must preserve the order of tasks given to it for a
     * given {@code Promise}. Otherwise you will run into unexpected behavior like messages arriving
     * out of order and your promise closing before all messages arrive. </p>
     *
     * <p> You should only set this once for your application (though not enforced). If you want
     * more granular control, use {@link me.tatarka.ipromise.Deferred.Builder#callbackExecutor(java.util.concurrent.Executor)}
     * or {@link Deferred#Deferred(me.tatarka.ipromise.buffer.PromiseBuffer, CancelToken,
     * java.util.concurrent.Executor)}. </p>
     *
     * @param callbackExecutor the callback executor
     */
    public static void setDefaultCallbackExecutor(Executor callbackExecutor) {
        defaultCallbackExecutor = callbackExecutor;
    }

    /**
     * Returns the the default callback executor that executors all callbacks in a single background
     * thread.
     *
     * @return the default callback executor
     */
    public static Executor getDefaultCallbackExecutor() {
        if (defaultCallbackExecutor == null)
            defaultCallbackExecutor = Executors.newSingleThreadExecutor();
        return defaultCallbackExecutor;
    }

    /**
     * Returns an executor that runs all callbacks in the same thread that they were sent from.
     * While this is not a good idea in you application code, it is nice to set this in unit tests
     * so that you can have the callback run immediately and not have to worry about synchronizing
     * threads.
     *
     * @return the callback executor
     */
    public static Executor getSameThreadCallbackExecutor() {
        if (sameThreadCallbackExecutor == null) sameThreadCallbackExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        return sameThreadCallbackExecutor;
    }

    protected Promise() {
        this(new CancelToken(), getDefaultCallbackExecutor());
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
     * Constructs a new {@code Promise} that completes after both the current and given promises
     * complete.
     *
     * @param promise the other {@code Promise}
     * @param <T2>    the type of the other {@code Promise}
     * @return the new {@code Promise}
     */
    public <T2> Promise<Pair<T, T2>> and(Promise<T2> promise) {
        return and(this, promise).then(new Map<List, Pair<T, T2>>() {
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
    public static Promise<List> and(final Promise... promises) {
        if (promises == null) throw new NullPointerException();

        final Promise<List> newPromise = new Promise<List>();
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
                public void receive(Object message) {
                    synchronized (lock) {
                        results.set(index, message);
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
                public void receive(T message) {
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
