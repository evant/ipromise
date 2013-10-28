package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.List;

/**
 * A promise is a way to return a result the will be fulfilled sometime in the future. This fixes
 * the inversion of control that callback-style functions creates and restores the composeability of
 * return values.
 * <p/>
 * A example of an asynchronous method is as follows:
 * <pre>
 *  async(arg, new Callback {
 *      {@literal @}Override
 *      public void onResult(Result result) {
 *          // Do something with result
 *      }
 *  );
 * </pre>
 * <p/>
 * With Promises, the asynchronous method looks much more like a standard method with a return
 * value.
 * <pre>
 *   Promise result = async(arg);
 *   result.listen(new Promise.Adapter() {
 *      {@literal @}Override
 *      public void success(Result result) {
 *          // Do something with result
 *      }
 *   });
 * </pre>
 * <p/>
 * In addition to creating a standard interface for all asynchronous functions, Promises can also
 * more-robustly handle error and cancellation situations.
 *
 * @author Evan Tatarka
 */
public class Promise<T, E extends Exception> {
    private CancelToken cancelToken;
    private List<Listener<T, E>> listeners = new ArrayList<Listener<T, E>>();
    private boolean delivered;
    private Result<T, E> result;

    /**
     * Constructs a new promise.
     */
    public Promise() {
        this(new CancelToken());
    }

    /**
     * Constructs a new promise with the given {@link CancelToken}. When the token is canceled, the
     * promise is also canceled.
     *
     * @param cancelToken the cancel token
     */
    public Promise(CancelToken cancelToken) {
        this.cancelToken = cancelToken;
        cancelToken.addListener(new CancelToken.Listener() {
            @Override
            public void onCancel() {
                cancel();
            }
        });
    }

    /**
     * Delivers a successful result to all listeners of the {@code Promise}. Only one result can be
     * delivered. If the promise has already been canceled, the result will not be stored and
     * listeners will not be notified.
     *
     * @param success the successful result to deliver.
     * @throws Promise.AlreadyDeliveredException throws if a result has already been delivered.
     */
    public synchronized void deliver(T success) {
        if (delivered) {
            if (result.isCanceled()) return;
            else throw new AlreadyDeliveredException(this, success);
        }

        result = Result.success(success);
        delivered = true;
        for (Listener<T, E> listener : listeners) {
            listener.result(result);
        }
        listeners.clear();
    }

    /**
     * Delivers an error result to all listeners of the {@code Promise}. Only one result can be
     * delivered. If the promise has already been canceled, the result will not be stored and
     * listeners will not be notified.
     *
     * @param error the error result to deliver.
     */
    public synchronized void deliver(E error) {
        if (delivered) {
            if (result.isCanceled()) return;
            else throw new AlreadyDeliveredException(this, error);
        }

        result = Result.error(error);
        delivered = true;
        for (Listener<T, E> listener : listeners) {
            listener.result(result);
        }
        listeners.clear();
    }

    /**
     * Delivers a result to all listeners of the {@code Promise}. This is a convenience method if
     * you already have a {@link Result} and want to pass it along to another promise.
     *
     * @param result the result to deliver.
     */
    public synchronized void deliver(Result<T, E> result) {
        try {
            deliver(result.get());
        } catch (Result.CanceledException e) {
            cancel();
        } catch (Exception e) {
            deliver((E) e);
        }
    }

    /**
     * Cancels the {@code Promise}, notifying all listeners and propagating the cancellation to all
     * Promises that share the {@link CancelToken}.
     */
    public synchronized void cancel() {
        if (delivered) return;

        result = Result.cancel();
        delivered = true;
        for (Listener<T, E> listener : listeners) {
            listener.result(result);
        }
        listeners.clear();
        cancelToken.cancel();
    }

    /**
     * Listens to a {@code Promise}, getting a result when the {@code Promise} completes. If the
     * {@code Promise} has already completed, the listener is immediately called. This way you can't
     * "miss" the result.
     *
     * @param listener the listener to call when the promise completes
     * @return the promise for chaining
     */
    public synchronized Promise<T, E> listen(Listener<T, E> listener) {
        if (delivered) {
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
     * @param map the function to map the result of the original {@code Promise} to the new promise
     * @param <B> the success result type of the new {@code Promise}
     * @return the new {@code Promise}
     */
    public <B> Promise<B, E> then(final Map<T, B> map) {
        final Promise<B, E> newPromise = new Promise<B, E>(cancelToken);
        listen(new Adapter<T, E>() {
            @Override
            public void success(T result) {
                newPromise.deliver(map.map(result));
            }

            @Override
            public void error(E error) {
                newPromise.deliver(error);
            }

            @Override
            public void canceled() {
                newPromise.cancel();
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
     * @param <B>   the type of the second {@code Promise} success result
     * @param <ER>  the type of the second {@code Promise} error result
     * @param <E2>  the type of the new {@code Promise} error result.The reason this is a separate
     *              type is to allow chaining of promises with different error types. Since the
     *              original error may be passed through, this must be a superclass of both the
     *              first and second {@code Promise} error types.
     * @return the new {@code Promise}
     */
    public <B, ER extends Exception, E2 extends ER> Promise<B, ER> then(final Chain<T, B, ER, E2> chain) {
        final Promise<B, ER> newPromise = new Promise<B, ER>(cancelToken);
        listen(new Adapter<T, E>() {
            @Override
            public void success(T result) {
                Promise<B, E2> chainedPromise = chain.chain(result);
                // Fighting the type system
                ((Promise<B, ER>) chainedPromise).listen(new Listener<B, ER>() {
                    @Override
                    public void result(Result<B, ER> result) {
                        newPromise.deliver(result);
                    }
                });
            }

            @Override
            public void error(E error) {
                newPromise.deliver((ER) error);
            }

            @Override
            public void canceled() {
                newPromise.cancel();
            }
        });
        return newPromise;
    }

    /**
     * A listener for receiving the result of a promise.
     *
     * @param <T> the success result type
     * @param <E> the error result type
     * @see Promise#listen(Promise.Listener)
     */
    public interface Listener<T, E extends Exception> {
        public void result(Result<T, E> result);
    }

    /**
     * This adapter is useful for when you only care about some of the possible result states or you
     * want to split out the possible result states in separate methods. If you override {@link
     * Adapter#result(Result)}, you must call {@code super} for the adapter to work correctly.
     *
     * @param <T> the success result type
     * @param <E> the error result type
     */
    public static abstract class Adapter<T, E extends Exception> implements Listener<T, E> {
        @Override
        public void result(Result<T, E> result) {
            try {
                success(result.get());
            } catch (Result.CanceledException e) {
                canceled();
            } catch (Exception e) {
                error((E) e);
            }
        }

        public void success(T result) {
        }

        public void error(E error) {
        }

        public void canceled() {
        }
    }

    /**
     * Maps the result of one {@code Promise} to the result of another {@code Promise}
     *
     * @param <A> the type of the original {@code Promise} success result
     * @param <B> the type of the new {@code Promise} success result
     * @see Promise#then(Promise.Map)
     */
    public interface Map<A, B> {
        public B map(A result);
    }

    /**
     * Chains the result of one {@code Promise} to second {@code Promise}.
     *
     * @param <A>  the type of the original {@code Promise} success result
     * @param <B>  the type of the second {@code Promise} success result
     * @param <ER> the type of the chain's error result. The must be a superclass of the error
     *             result of the first and second {@code Promise}.
     * @param <E>  the type of the second {@Code Promise} error result
     * @see Promise#then(Promise.Chain)
     */
    public interface Chain<A, B, ER extends Exception, E extends ER> {
        public Promise<B, E> chain(A result);
    }

    /**
     * The Exception thrown if a result has already been delivered and a second result is attempted
     * to be delivered.
     */
    public static class AlreadyDeliveredException extends IllegalStateException {
        public AlreadyDeliveredException(Promise promise, Object result) {
            super("Result: " + result + " cannot be delivered because " + promise.result + " has already been delivered.");
        }
    }
}
