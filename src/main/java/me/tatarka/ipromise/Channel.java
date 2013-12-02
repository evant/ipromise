package me.tatarka.ipromise;

/**
 * A {@code Channel} is the producer end of a {@code Progress}. An asynchronous method creates a
 * {@code Channel} and returns {@link Progress}, then calls {@link Channel#send(Object)} to update
 * the progress.
 *
 * @param <T> the type of a message
 */
public class Channel<T> {
    private Progress<T> progress;

    public Channel() {
        progress = new Progress<T>();
    }

    public Channel(CancelToken cancelToken) {
        progress = new Progress<T>(cancelToken);
    }

    public Progress<T> progress() {
        return progress;
    }

    /**
     * Sends a message to the listener of the {@code Progress}. If the progress has been canceled,
     * the message will be ignored.
     *
     * @param message the message to send
     */
    public void send(T message) {
        progress.deliver(message);
    }
}
