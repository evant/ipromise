package me.tatarka.ipromise;

/**
 * A way to control an async operation.
 *
 * @param <T> the result type
 */
public interface Task<T> {
    /**
     * Starts the async operation. This method is expected to return immediately, running the
     * operation asynchronously.
     *
     * @return an {@link me.tatarka.ipromise.Async} to manage the operation's result.
     */
    Async<T> start();
}
