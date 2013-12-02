package me.tatarka.ipromise;

/**
 * Chains the result of one thing to another
 *
 * @param <T1> the type of the original value
 * @param <T2> the type of the second value
 */
public interface Chain<T1, T2> {
    T2 chain(T1 chain);
}
