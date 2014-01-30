package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A promise is a way to return a result the will be fulfilled sometime in the future. This fixes
 * the inversion of control that callback-style functions creates and restores the composeability of
 * return values.
 *
 * In addition to creating a standard interface for all asynchronous functions, Promises can also
 * more-robustly handle error and cancellation situations.
 *
 * You cannot construct a {@code Promise} directly, instead you must get one from a {@link
 * Deferred}. That is, unless the result is already available.
 *
 * @author Evan Tatarka
 * @see Deferred
 */
public class Promise<T> {
    private CancelToken cancelToken;
    private List<Listener<T>> listeners = new ArrayList<Listener<T>>();
    private T result;
    private boolean isFinished;

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
        this.cancelToken = cancelToken;
    }

    /**
     * Constructs a promise with a result already in it. This is useful for when you can return the
     * value immediately.
     *
     * @param result the result
     */
    public Promise(T result) {
        cancelToken = new CancelToken();
        this.result = result;
        isFinished = true;
    }

    /**
     * Delivers a result to all listeners of the {@code Promise}. This is used internally by {@link
     * Deferred}.
     *
     * @param result the result to reject.
     */
    synchronized void deliver(T result) {
        if (isFinished) throw new AlreadyDeliveredException(this, result);
        if (cancelToken.isCanceled()) return;

        this.result = result;
        isFinished = true;
        for (Listener<T> listener : listeners) {
            listener.receive(result);
        }
        listeners.clear();
    }

    /**
     * Cancels the {@code Promise}, notifying all listeners and propagating the cancellation to all
     * Promises that share the {@link CancelToken}.
     */
    public synchronized void cancel() {
        if (!isFinished) {
            listeners.clear();
            cancelToken.cancel();
        }
    }

    /**
     * Returns if the promise has finished and is holding a result.
     *
     * @return true if finished, false otherwise
     */
    public synchronized boolean isFinished() {
        return isFinished;
    }

    /**
     * Returns if the promise has been canceled.
     *
     * @return true if canceld, false otherwise
     */
    public synchronized boolean isCanceled() {
        return cancelToken.isCanceled();
    }

    /**
     * Returns the result if the promise has finished. Use {@link Promise#isFinished()} to ensure
     * the result has been isFinished.
     *
     * @return the result if the promise has finished
     * @throws NotFinishedException if the promise has not isFinished the result yet.
     */
    public synchronized T tryGet() throws NotFinishedException {
        if (isFinished) {
            return result;
        } else {
            throw new NotFinishedException(this, "cannot get receive");
        }
    }

    /**
     * Listens to a {@code Promise}, getting a result when the {@code Promise} completes. If the
     * {@code Promise} has already completed, the listener is immediately called. This way you can't
     * "miss" the result.
     *
     * @param listener the listener to call when the promise completes
     * @return the promise for chaining
     */
    public synchronized Promise<T> listen(Listener<T> listener) {
        if (listener == null) return this;

        if (isFinished) {
            listener.receive(result);
        } else {
            listeners.add(listener);
        }

        return this;
    }

    /**
     * Constructs a new promise that returns when the original promise returns but passes the result
     * through the given {@link Map} function.
     *
     * @param map  the function to chain the result of the original {@code Promise} to the new
     *             promise
     * @param <T2> the result type of the new {@code Promise}
     * @return the new {@code Promise}
     */
    public <T2> Promise<T2> then(final Map<T, T2> map) {
        final Promise<T2> newPromise = new Promise<T2>(cancelToken);
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
                newPromise.deliver(map.map(result));
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
        final Promise<T2> newPromise = new Promise<T2>(cancelToken);
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
                final Promise<T2> chainedPromise = chain.chain(result);
                CancelToken.join(cancelToken, chainedPromise.cancelToken);
                chainedPromise.listen(new Listener<T2>() {
                    @Override
                    public void receive(T2 result) {
                        newPromise.deliver(result);
                    }
                });
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
        return and(this, promise).then(new Map<Object[], Pair<T, T2>>() {
            @Override
            public Pair<T, T2> map(Object[] result) {
                return Pair.of((T) result[0], (T2) result[1]);
            }
        });
    }

    /**
     * Constructs a new {@code Promise} that completes after all the given promises complete. This
     * method can take any number of promises; however, the returned promise is not strongly typed.
     * Therefore, you only have 2 promises you should use {@link Promise#and(Promise)} instead.
     *
     * @param promises the promises
     * @return the new {@code Promise}
     */
    public static Promise<Object[]> and(final Promise... promises) {
        if (promises == null) throw new NullPointerException();

        final Promise<Object[]> newPromise = new Promise<Object[]>();
        final AtomicInteger count = new AtomicInteger();
        final Object[] results = new Object[promises.length];
        final Object lock = new Object();

        for (int i = 0; i < promises.length; i++) {
            final int index = i;
            CancelToken.join(newPromise.cancelToken, promises[index].cancelToken);
            promises[index].listen(new Listener() {
                @Override
                public void receive(Object result) {
                    synchronized (lock) {
                        results[index] = result;
                        int done = count.incrementAndGet();

                        if (done == promises.length) {
                            newPromise.deliver(results);
                        }
                    }
                }
            });
        }

        return newPromise;
    }

    /**
     * Constructs a new {@code Promise} that completes when either the current or given promises
     * completes. The other promise is then canceled.
     *
     * @param promise the other {@code Promise}
     * @return the new {@code Promise}
     */
    public Promise<T> or(final Promise<T> promise) {
        return or(this, promise);
    }

    /**
     * Constructs a new {@code Promise} that completes when one of the given promises completes. The
     * rest are then canceled.
     *
     * @param promises the promises
     * @param <T>      the type of the promises
     * @return the new {@code Promise}
     */
    public static <T> Promise<T> or(final Promise<T>... promises) {
        if (promises == null) throw new NullPointerException();

        final Promise<T> newPromise = new Promise<T>();
        final AtomicBoolean finished = new AtomicBoolean();
        final AtomicInteger canceledCount = new AtomicInteger();

        newPromise.cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                for (Promise<T> promise : promises) promise.cancel();
            }
        });

        for (int i = 0; i < promises.length; i++) {
            final int index = i;
            // We can't just join cancel tokens here because the new promise should only cancel if
            // all of the given promises are canceled.
            promises[index].cancelToken.listen(new CancelToken.Listener() {
                @Override
                public void canceled() {
                    int count = canceledCount.incrementAndGet();
                    if (count == promises.length) newPromise.cancel();
                }
            });

            promises[index].listen(new Listener<T>() {
                @Override
                public void receive(T result) {
                    if (!finished.getAndSet(true)) {
                        newPromise.deliver(result);
                        for (int j = 0; j < promises.length; j++) {
                            if (index != j) promises[j].cancel();
                        }
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
    public static class AlreadyDeliveredException extends IllegalStateException {
        public AlreadyDeliveredException(Promise promise, Object result) {
            super(result + " cannot be isFinished because " + promise.result + " has already been isFinished.");
        }
    }

    /**
     * The Exception thrown if the result is not yet available.
     */
    public static class NotFinishedException extends IllegalStateException {
        public NotFinishedException(Promise promise, String message) {
            super(message + " because promise has not finished");
        }
    }

}
