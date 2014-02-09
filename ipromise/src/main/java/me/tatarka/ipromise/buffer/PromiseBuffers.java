package me.tatarka.ipromise.buffer;

import me.tatarka.ipromise.Promise;

/**
 * Useful utility methods for creating a {@link me.tatarka.ipromise.buffer.PromiseBuffer}.
 *
 * @author Evan Tatarka
 */
public class PromiseBuffers {
    private PromiseBuffers() {
    }

    /**
     * Creates a {@link me.tatarka.ipromise.buffer.PromiseBuffer} that stores no messages. This
     * means that if there is no {@link me.tatarka.ipromise.Listener} attached when the message is
     * sent, it will be dropped. Use this if you don't care if messages are dropped.
     *
     * @param <T> the message type
     * @return the {@link me.tatarka.ipromise.buffer.PromiseBuffer}
     */
    public static <T> PromiseBuffer<T> none() {
        return new RingPromiseBuffer<T>(0);
    }

    /**
     * Creates a {@link me.tatarka.ipromise.buffer.PromiseBuffer} that only stores the last message
     * that was sent. The is the default if no buffer is specified because it most closely matches a
     * standard Future implementation when only one message is sent.
     *
     * @param <T> the message type
     * @return the {@link me.tatarka.ipromise.buffer.PromiseBuffer}
     */
    public static <T> PromiseBuffer<T> last() {
        return new RingPromiseBuffer<T>(1);
    }

    /**
     * Creates a {@link me.tatarka.ipromise.buffer.PromiseBuffer} that stores all messages. Use this
     * if you want to be sure that no messages will be dropped. Note: be careful about memory usage
     * since all messages will be kept in memory until the {@link me.tatarka.ipromise.Promise} is
     * garbage collected.
     *
     * @param <T> the message type
     * @return the {@link me.tatarka.ipromise.buffer.PromiseBuffer}
     */
    public static <T> PromiseBuffer<T> all() {
        return new ArrayPromiseBuffer<T>();
    }

    /**
     * Creates a {@link me.tatarka.ipromise.buffer.PromiseBuffer} that stores messages in a ring
     * buffer of the given capacity. In other words, when there is no more room for a message, it
     * will replace the oldest once. Use this if you want to allow some messages to drop if you get
     * too many.
     *
     * @param capacity the number of messages received before it will start replacing old ones
     * @param <T>      the message type
     * @return the {@link me.tatarka.ipromise.buffer.PromiseBuffer}
     */
    public static <T> PromiseBuffer<T> ring(final int capacity) {
        return new RingPromiseBuffer<T>(capacity);
    }

    /**
     * Creates a {@link me.tatarka.ipromise.buffer.PromiseBuffer} that stores a number of messages
     * up to the given capacity. Sending any more messages will cause an {@link
     * java.lang.IllegalStateException} to be thrown. Use this is you know exactly how many messages
     * will be sent and you want to enforce that.
     *
     * @param capcity the maximum number of message
     * @param <T>     the message type
     * @return the {@link me.tatarka.ipromise.buffer.PromiseBuffer}
     */
    public static <T> PromiseBuffer<T> fixed(final int capcity) {
        return new ArrayPromiseBuffer<T>(capcity);
    }

    /**
     * Returns a {@link me.tatarka.ipromise.buffer.PromiseBuffer}  for the given enumeration. This
     * is a convenience method so that you can use an enumeration directly for common buffer types.
     *
     * @param type The buffer type. Valid values are {@link Promise#BUFFER_NONE}, {@link
     *             Promise#BUFFER_LAST}, and {@link Promise#BUFFER_ALL}.
     * @param <T>  the message type
     * @return the {@link me.tatarka.ipromise.buffer.PromiseBuffer}
     * @see Promise#BUFFER_NONE
     * @see Promise#BUFFER_LAST
     * @see Promise#BUFFER_ALL
     */
    public static <T> PromiseBuffer<T> ofType(int type) {
        switch (type) {
            case Promise.BUFFER_NONE:
                return none();
            case Promise.BUFFER_LAST:
                return last();
            case Promise.BUFFER_ALL:
                return all();
            default:
                throw new IllegalArgumentException("Unknown promise buffer type: " + type);
        }
    }

}
