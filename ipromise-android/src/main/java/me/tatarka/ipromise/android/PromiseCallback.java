package me.tatarka.ipromise.android;

public interface PromiseCallback<T> {
    void receive(T result);
}
