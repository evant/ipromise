package me.tatarka.ipromise.android;

import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Task;

/**
 * Created by evan
 */
interface IPromiseManager {
    <T> Promise<T> get(String tag);
    void put(String tag, Promise<?> promise);
    void cancelAll();
}
