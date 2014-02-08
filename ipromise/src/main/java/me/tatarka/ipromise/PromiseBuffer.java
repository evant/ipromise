package me.tatarka.ipromise;

/**
 * Created by evan
 */
public interface PromiseBuffer<T> extends Iterable<T> {
    public void add(T item);
}
