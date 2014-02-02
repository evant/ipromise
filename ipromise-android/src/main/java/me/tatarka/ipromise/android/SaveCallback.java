package me.tatarka.ipromise.android;

import android.os.Bundle;

public interface SaveCallback<T> {
    void asyncSave(T result, Bundle outState);
    T asyncRestore(Bundle savedState);
}
