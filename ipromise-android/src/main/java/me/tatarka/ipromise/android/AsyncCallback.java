package me.tatarka.ipromise.android;

public interface AsyncCallback<T> {
    void start();
    void receive(T result);
    void end();
}
