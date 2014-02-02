package me.tatarka.ipromise.android;

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
}
