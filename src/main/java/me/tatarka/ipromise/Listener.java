package me.tatarka.ipromise;

/**
 * A listener for receiving a result.
 *
 * @param <T> the result type
 */
public interface Listener<T> {
    public void receive(T result);
}
