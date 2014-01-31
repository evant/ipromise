package me.tatarka.ipromise.android;

public interface PromiseCallback<T> {
    void start();
    void receive(T result);
}
