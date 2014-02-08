package me.tatarka.ipromise.android;

import me.tatarka.ipromise.Promise;

/**
 * @author Evan Tatarka
 */
interface IAsyncManager {
    <T> Promise<T> get(String tag);

    <T> void put(String tag, Promise<T> async);

    void cancelAll();

    <T> void save(String tag, SaveCallback<T> callback);

    <T> T restore(String tag, SaveCallback<T> callback);
}
