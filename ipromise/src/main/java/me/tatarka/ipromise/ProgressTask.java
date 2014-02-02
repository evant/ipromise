package me.tatarka.ipromise;

/**
 * A {@link me.tatarka.ipromise.Task} for a {@link me.tatarka.ipromise.Progress}.
 *
 * @param <T> the message type
 */
public interface ProgressTask<T> extends Task<T> {
    @Override
    Progress<T> start();

    /**
     * A callback for a {@code ProgressTask}. The task controls in what context the callback is
     * run.
     *
     * @param <T> the message type
     */
    public static interface Do<T> {
        /**
         * Called to executor the callback.
         *
         * @param channel     the channel to send message to
         * @param cancelToken the cancel token to listen for cancellations
         */
        void run(Channel<T> channel, CancelToken cancelToken);
    }
}
