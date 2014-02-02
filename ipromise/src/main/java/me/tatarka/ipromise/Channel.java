package me.tatarka.ipromise;

/**
 * A {@code Channel} is the producer end of a {@code Progress}. An asynchronous method creates a
 * {@code Channel} and returns {@link Progress}, then calls {@link Channel#send(Object)} to update
 * the progress.
 *
 * @param <T> the type of a message
 * @author Evan Tatarka
 */
public class Channel<T> {
    private Progress<T> progress;

    /**
     * Constructs a new {@code Channel} with a default retention policy of {@link
     * me.tatarka.ipromise.Progress#RETAIN_LAST}.
     */
    public Channel() {
        progress = new Progress<T>();
    }

    /**
     * Constructs a new {@code Channel} with the given retention policy. Valid values are {@link
     * me.tatarka.ipromise.Progress#RETAIN_NONE}, {@link me.tatarka.ipromise.Progress#RETAIN_LAST},
     * and {@link me.tatarka.ipromise.Progress#RETAIN_ALL}.
     *
     * @param retentionPolicy the retention policy
     */
    public Channel(int retentionPolicy) {
        progress = new Progress<T>(retentionPolicy);
    }

    /**
     * Constructs a new {@code Channel} with the given {@link me.tatarka.ipromise.CancelToken} and a
     * default retention policy of {@link me.tatarka.ipromise.Progress#RETAIN_LAST}.
     *
     * @param cancelToken the cancel token
     */
    public Channel(CancelToken cancelToken) {
        progress = new Progress<T>(cancelToken);
    }

    /**
     * Constructs a new {@code Channel} with the given retention policy and {@link
     * me.tatarka.ipromise.CancelToken}. Valid values are {@link me.tatarka.ipromise.Progress#RETAIN_NONE},
     * {@link me.tatarka.ipromise.Progress#RETAIN_LAST}, and {@link me.tatarka.ipromise.Progress#RETAIN_ALL}.
     *
     * @param retentionPolicy the retention policy
     * @param cancelToken     the cancel token
     */
    public Channel(int retentionPolicy, CancelToken cancelToken) {
        progress = new Progress<T>(retentionPolicy, cancelToken);
    }

    /**
     * Returns the channel's {@link me.tatarka.ipromise.Progress}.
     *
     * @return the progress
     * @throws me.tatarka.ipromise.Channel.ClosedChannelException if the channel has already been
     *                                                            closed.
     */
    public Progress<T> progress() {
        if (progress == null) {
            throw new ClosedChannelException();
        }
        return progress;
    }

    /**
     * Sends a message to the listener of the {@link me.tatarka.ipromise.Progress}. If the progress
     * has been canceled, the message will be ignored.
     *
     * @param message the message to send
     * @throws me.tatarka.ipromise.Channel.ClosedChannelException if the channel has already been
     *                                                            closed.
     */
    public void send(T message) {
        if (progress == null) {
            throw new ClosedChannelException();
        }
        progress.deliver(message);
    }

    /**
     * Notifies the {@link me.tatarka.ipromise.Progress} that no more messages will be sent.
     */
    public void close() {
        if (progress != null) {
            progress.close();
            progress = null;
        }
    }

    public static class ClosedChannelException extends IllegalStateException {
        public ClosedChannelException() {
            super("channel has been closed");
        }
    }
}
