package me.tatarka.ipromise;


public interface Fold<T, R> {
    R fold(R accumulator, T item);
}
