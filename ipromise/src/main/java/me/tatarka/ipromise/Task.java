package me.tatarka.ipromise;

/**
 * An easy way to start run something asynchronously and return a {@link
 * me.tatarka.ipromise.Promise}.
 *
 * @param <T> the result type
 */
public interface Task<T> {
    Async<T> start();
}
