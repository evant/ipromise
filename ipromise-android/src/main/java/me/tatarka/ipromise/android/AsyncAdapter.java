package me.tatarka.ipromise.android;

import android.os.Bundle;
import android.os.Parcelable;

/**
 * An adapter for {@link me.tatarka.ipromise.android.AsyncCallback} so you don't have to implement
 * all it's methods.
 *
 * @author Evan Tatarka
 */
public abstract class AsyncAdapter<T> implements AsyncCallback<T> {
    @Override
    public void start() {

    }

    @Override
    public void end() {

    }

    @Override
    public void save(T result, Bundle outState) {

    }

    @Override
    public T restore(Bundle savedState) {
        return null;
    }
}
