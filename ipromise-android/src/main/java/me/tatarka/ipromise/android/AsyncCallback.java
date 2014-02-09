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
     * Called when the {@link me.tatarka.ipromise.task.Task} is started with {@link
     * AsyncItem#start()} or {@link AsyncItem#restart()}, or when there is a configuration change
     * and the {@code Task} has not completed. This is where you would change your UI state to show
     * that an async operation is pending (e.x. show a progress bar).
     */
    void start();

    /**
     * Called when the {@link me.tatarka.ipromise.Promise} has received a message, or on a
     * configuration change when the {@code Promise} has a buffered message. This is where you would
     * change your UI to show the result of {@code Promise}.
     *
     * @param message the result
     */
    void receive(T message);

    /**
     * Called when the {@link me.tatarka.ipromise.Promise} is closed, or on a configuration change
     * after the {@code Promise} has been closed. This is where you would change your UI to show the
     * completion of the {@code Promise}.
     */
    void close();
}
