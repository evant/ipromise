package me.tatarka.ipromise.android;

import android.os.Bundle;

/**
 * Save the result of a {@link me.tatarka.ipromise.task.Task} so that it persists when the Activity is
 * destroyed. If a result is saved, {@link me.tatarka.ipromise.android.AsyncCallback#receive(Object)}
 * will be called with the result when the Activity is recreated.
 *
 * @param <T> the result type
 */
public interface SaveCallback<T> {
    void asyncSave(T result, Bundle outState);

    T asyncRestore(Bundle savedState);
}
