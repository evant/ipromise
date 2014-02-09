package me.tatarka.ipromise.func;

/**
 * Filters a set of items. Returning true will keep the item, returning false will drop it.
 *
 * @param <T> the type to filter on
 * @author Evan Tatarka
 * @see me.tatarka.ipromise.Promise#then(Filter)
 */
public interface Filter<T> {
    boolean filter(T item);
}
