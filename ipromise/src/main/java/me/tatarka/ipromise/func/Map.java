package me.tatarka.ipromise.func;

/**
 * Maps the result of one {@code Promise} to the result of another {@code Promise}
 *
 * @param <T1> the type of the original {@code Promise} result
 * @param <T2> the type of the new {@code Promise} result
 * @author Evan Tatarka
 * @see me.tatarka.ipromise.Promise#then(Map)
 */
public interface Map<T1, T2> {
    public T2 map(T1 result);
}
