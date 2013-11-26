package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.List;

/**
 * T1 promise is a way to return a result the will be fulfilled sometime in the future. This fixes
 * the inversion of control that callback-style functions creates and restores the composeability of
 * return values.
 * <p/>
 * T1 example of an asynchronous method is as follows:
 * <pre>{@code
 *  async(arg, new Callback {
 *      @literal@Override
 *      public void onResult(Result result) {
 *          // Do something with result
 *      }
 *  );
 * }</pre>
 * <p/>
 * With Promises, the asynchronous method looks much more like a standard method with a return
 * value.
 * <pre>{@code
 *   Promise<Result, Error> result = async(arg);
 *   result.listen(new Promise.Adapter<Result, Error>() {
 *      @literal@Override
 *      public void success(Result result) {
 *          // Do something with result
 *      }
 *   });
 * }</pre>
 * <p/>
 * In addition to creating a standard interface for all asynchronous functions, Promises can also
 * more-robustly handle error and cancellation situations.
 * <p/>
 * You cannot construct a {@code Promise} directly, instead you must get one from a {@link
 * Deferred}.
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
    Promise() {
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
        cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                cancel();
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
        for (Promise.Listener<T> listener : listeners) {
            listener.result(result);
        }
        listeners.clear();
    }

    /**
     * Cancels the {@code Promise}, notifying all listeners and propagating the cancellation to all
     * Promises that share the {@link CancelToken}.
     */
    public synchronized void cancel() {
        if (!isFinished) cancelToken.cancel();
    }

    /**
     * Returns if the promise has finished and is holding a result.
     *
     * @return true if finished, false otherwise
     */
    public synchronized boolean isFinished() {
        return isFinished;
    }

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
    public synchronized T getResult() throws NotFinishedException {
        if (isFinished) {
            return result;
        } else {
            throw new NotFinishedException(this, "cannot get result");
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
        if (isFinished) {
            listener.result(result);
        } else {
            listeners.add(listener);
        }
        return this;
    }

    /**
     * Constructs a new promise that returns when the original promise returns but, passes the
     * result through the given {@link Promise.Map} function. If the result of the original {@code
     * Promise} is not successful, the error or cancellation is passed through to the new {@code
     * Promise} without invoking the {@link Promise.Map} function. This way you can do some work on
     * an asynchronous result and pass it along without worrying about error or cancel states.
     *
     * @param map  the function to map the result of the original {@code Promise} to the new
     *             promise
     * @param <T2> the success result type of the new {@code Promise}
     * @return the new {@code Promise}
     */
    public <T2> Promise<T2> then(final Map<T, T2> map) {
        final Promise<T2> newPromise = new Promise<T2>(cancelToken);
        listen(new Listener<T>() {
            @Override
            public void result(T result) {
                newPromise.deliver(map.map(result));
            }
        });
        return newPromise;
    }

    /**
     * Constructs a new {@code Promise} that chains two promises in succession. If the first {@code
     * Promise} is not successful, the resulting {@code Promise} immediately receives the error or
     * cancellation and the second {@code Promise} is never called.
     *
     * @param chain the {@link Promise.Chain} that constructs the second {@code Promise}
     * @param <T2>  the type of the second {@code Promise} success result
     * @return the new {@code Promise}
     */
    public <T2> Promise<T2> then(final Chain<T, T2> chain) {
        final Promise<T2> newPromise = new Promise<T2>(cancelToken);
        listen(new Listener<T>() {
            @Override
            public void result(T result) {
                final Promise<T2> chainedPromise = chain.chain(result);
                CancelToken.join(cancelToken, chainedPromise.cancelToken);
                chainedPromise.listen(new Listener<T2>() {
                    @Override
                    public void result(T2 result) {
                        newPromise.deliver(result);
                    }
                });
            }
        });
        return newPromise;
    }

    /**
     * Forces a cast of a promise to a super-type to get around covariance restrictions. {@code T2}
     * and {@code E2} must be a superclass of {@code T} and {@code E} respectively. This is safe
     * because you can't directly deliver results to the cast promise.
     *
     * @param <T2> the type of the success value to cast to.
     * @return a cast of the promise.
     */
    public <T2> Promise<T2> cast() {
        return (Promise<T2>) this;
    }

    /**
     * T1 listener for receiving the result of a promise.
     *
     * @param <T> the success result type
     * @see Promise#listen(Promise.Listener)
     */
    public interface Listener<T> {
        public void result(T result);
    }

    /**
     * Maps the result of one {@code Promise} to the result of another {@code Promise}
     *
     * @param <T1> the type of the original {@code Promise} success result
     * @param <T2> the type of the new {@code Promise} success result
     * @see Promise#then(Promise.Map)
     */
    public interface Map<T1, T2> {
        public T2 map(T1 result);
    }

    /**
     * Chains the result of one {@code Promise} to second {@code Promise}.
     *
     * @param <T1> the type of the original {@code Promise} result
     * @param <T2> the type of the second {@code Promise} result
     * @see Promise#then(Promise.Chain)
     */
    public interface Chain<T1, T2> {
        public Promise<T2> chain(T1 result);
    }

    /**
     * The Exception thrown if a result has already been isFinished and a second result is attempted
     * to be isFinished.
     */
    public static class AlreadyDeliveredException extends IllegalStateException {
        public AlreadyDeliveredException(Promise promise, Object result) {
            super(result + " cannot be isFinished because " + promise.result + " has already been isFinished.");
        }
    }

    public static class NotFinishedException extends IllegalStateException {
        public NotFinishedException(Promise promise, String message) {
            super(message + " because promise has not finished");
        }
    }
}
