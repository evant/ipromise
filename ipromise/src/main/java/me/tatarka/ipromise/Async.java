package me.tatarka.ipromise;

/**
 * An asynchronous object. Both {@link me.tatarka.ipromise.Promise} and {@link
 * me.tatarka.ipromise.Progress} implement this interface.
 *
 * @author Evan Tatarka
 */
public interface Async<T> {
    /**
     * Register a callback.
     *
     * @param listener the listener
     * @return the {@code Async} for chaining
     */
    Async<T> listen(Listener<T> listener);

    /**
     * Returns if the async is still running.
     *
     * @return true if running, false otherwise
     */
    boolean isRunning();

    /**
     * Returns a cancel token. Canceling the cancel token should notify the {@code Async} to not
     * return any more results.
     *
     * @return the cancel token
     */
    CancelToken cancelToken();
}
