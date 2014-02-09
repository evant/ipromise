package me.tatarka.ipromise.func;

/**
 * Chains the result of one thing to another
 *
 * @param <T1> the type of the original value
 * @param <T2> the type of the second value
 * @author Evan Tatarka
 * @see me.tatarka.ipromise.Promise#then(Chain)
 */
public interface Chain<T1, T2> {
    T2 chain(T1 result);
}
