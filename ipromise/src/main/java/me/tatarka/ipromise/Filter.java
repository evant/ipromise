package me.tatarka.ipromise;

/**
 * Filters a set of items. Returning true will keep the item, returning false will drop it.
 *
 * @param <T> the type to filter on
 * @author Evan Tatarka
 */
public interface Filter<T> {
    boolean filter(T item);
}
