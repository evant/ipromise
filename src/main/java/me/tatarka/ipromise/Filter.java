package me.tatarka.ipromise;

public interface Filter<T> {
    boolean filter(T item);
}
