package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code CancelToken} is used to propagate cancellation of a {@link Promise}. All promises
 * sharing a token will be canceled if they have not yet been completed. Additionally, asynchronous
 * code that returns a promise can hold on to the {@code CancelToken} to cancel the promise at a
 * later time.
 *
 * @author Evan Tatarka
 */
public final class CancelToken {
    private boolean isCanceled;
    private List<Listener> listeners = new ArrayList<Listener>();

    /**
     * Cancels the token, notifying all promises of the cancellation.
     */
    public synchronized void cancel() {
        if (isCanceled) return;

        isCanceled = true;
        for (Listener listener : listeners) {
            listener.onCancel();
        }
        listeners.clear();
    }

    /**
     * Returns if the token has been canceled.
     *
     * @return true if canceled, false otherwise
     */
    public synchronized boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Adds a listener that will be notified when the token is canceled.
     *
     * @param listener the listener
     */
    public synchronized void addListener(Listener listener) {
        if (isCanceled) {
            listener.onCancel();
        } else {
            listeners.add(listener);
        }
    }

    /**
     * The listener to be notified when a token is canceled.
     */
    public interface Listener {
        void onCancel();
    }
}
