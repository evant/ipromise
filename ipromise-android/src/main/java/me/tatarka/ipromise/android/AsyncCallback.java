package me.tatarka.ipromise.android;

/**
 * The callback registered to {@link me.tatarka.ipromise.android.AsyncManager}. This will allow you
 * to manage the display state of your Activity or Fragment based on the state of the async
 * operation. All callbacks run on the UI thread.
 *
 * @param <T> the result type
 * @author Evan Tatarka
 */
public interface AsyncCallback<T> {
    /**
     * Called when the async operation is started with {@link me.tatarka.ipromise.android.AsyncManager#init}
     * or {@link me.tatarka.ipromise.android.AsyncManager#restart}, or when there is a configuration
     * change and the async operation has not completed. This is where you would change your UI
     * state to show that an async operation is pending (e.x. show a progress bar).
     */
    void start();

    /**
     * Called when the async operation has received a result, or on a configuration change when the
     * async operation has a result. This is where you would change your UI to show the result of
     * the async operation.
     *
     * @param result the result
     */
    void receive(T result);

    /**
     * This is only called for a {@link me.tatarka.ipromise.Progress}. Called when the async
     * operation has completed, or on a configuration change when the async operation has completed.
     * This is where you would change your UI to show the completion of the async operation.
     */
    void end();
}
