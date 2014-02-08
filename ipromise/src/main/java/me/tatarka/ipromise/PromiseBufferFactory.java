package me.tatarka.ipromise;

public interface PromiseBufferFactory {
    <T> PromiseBuffer<T> create();
}
