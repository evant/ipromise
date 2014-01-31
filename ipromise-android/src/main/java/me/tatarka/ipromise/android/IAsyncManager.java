package me.tatarka.ipromise.android;

import me.tatarka.ipromise.Async;

/**
 * Created by evan
 */
interface IAsyncManager {
    <T> Async<T> get(String tag);
    void put(String tag, Async<?> promise);
    void cancelAll();
}
