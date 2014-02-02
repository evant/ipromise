package me.tatarka.ipromise.android;

import me.tatarka.ipromise.Async;

/**
 * @author Evan Tatarka
 */
interface IAsyncManager {
    <T> Async<T> get(String tag);

    <T> void put(String tag, Async<T> promise);

    void cancelAll();
}
