package me.tatarka.ipromise;

/**
 * A {@code Deferred} is the producer end of a {@link Promise}. An asynchronous method creates a
 * {@code Deferred} and returns {@link Deferred#promise()}, then calls {@link
 * Deferred#resolve(Object)} at a later time.
 *
 * @param <T> the type of a result
 */
public class Deferred<T> {
    private Promise<T> promise;

    public Deferred() {
        promise = new Promise<T>();
    }

    public Deferred(CancelToken cancelToken) {
        promise = new Promise<T>(cancelToken);
    }

    public Promise<T> promise() {
        return promise;
    }

    /**
     * Delivers a result to all listeners of the {@code Promise}. Only one result can be delivered.
     * If the promise has already been canceled, the result will not be stored and listeners will
     * not be notified.
     *
     * @param result the result to reject.
     * @throws Promise.AlreadyDeliveredException throws if a result has already been delivered.
     */
    public synchronized void resolve(T result) {
        promise.deliver(result);
    }
}
