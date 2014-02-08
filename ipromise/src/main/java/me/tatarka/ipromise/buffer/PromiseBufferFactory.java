package me.tatarka.ipromise.buffer;

public interface PromiseBufferFactory {
    <T> PromiseBuffer<T> create();
}
