package me.tatarka.ipromise.android;

import me.tatarka.ipromise.Result;

/**
 * If you want to get separate callbacks for success and failure for a result type, you can use this
 * instead of {@link me.tatarka.ipromise.android.AsyncAdapter}.
 *
 * @author Evan Tatarka
 */
public abstract class ResultAsyncAdapter<T, E extends Exception> extends AsyncAdapter<Result<T, E>> {
    @Override
    public final void receive(Result<T, E> message) {
        result(message);
        if (message.isSuccess()) {
            success(message.getSuccess());
        } else {
            error(message.getError());
        }
    }

    /**
     * Called when a successful result is received.
     *
     * @param success the result
     */
    public abstract void success(T success);

    /**
     * Called when an error result is received.
     *
     * @param error the result
     */
    public abstract void error(E error);

    /**
     * Called when a result is received.
     *
     * @param result the result
     */
    public void result(Result<T, E> result) {

    }
}
