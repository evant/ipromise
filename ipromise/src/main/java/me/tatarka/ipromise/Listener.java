package me.tatarka.ipromise;

/**
 * A listener for receiving a result.
 *
 * @param <T> the result type
 * @author Evan Tatarka
 */
public interface Listener<T> {
    public void receive(T message);
}
