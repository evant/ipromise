package me.tatarka.ipromise;

import java.lang.ref.WeakReference;
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
            listener.canceled();
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
    public synchronized void listen(Listener listener) {
        if (isCanceled) {
            listener.canceled();
        } else {
            listeners.add(listener);
        }
    }

    /**
     * Combines the given cancel tokens so that the first one will cancel the second and vice-versa.
     * The join uses weak references so that one token will not stop the other from being garbage
     * collected.
     *
     * @param cancelTokens the cancel tokens
     */
    public static void join(CancelToken... cancelTokens) {
        final WeakReference<CancelToken>[] weakTokens = new WeakReference[cancelTokens.length];
        for (int i = 0; i < weakTokens.length; i++) weakTokens[i] = new WeakReference<CancelToken>(cancelTokens[i]);

        for (int i = 0; i < weakTokens.length; i++) {
            final int index = i;
            cancelTokens[i].listen(new Listener() {
                @Override
                public void canceled() {
                    for (int i = 0; i < weakTokens.length; i++) {
                        if (i == index) continue;
                        CancelToken token = weakTokens[i].get();
                        if (token != null) token.cancel();
                    }
                }
            });
        }
    }

    /**
     * The listener to be notified when a token is canceled.
     */
    public interface Listener {
        void canceled();
    }
}
