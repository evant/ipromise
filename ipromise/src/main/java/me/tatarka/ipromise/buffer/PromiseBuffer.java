package me.tatarka.ipromise.buffer;

/**
 * A {@code PromiseBuffer} defines how a {@link me.tatarka.ipromise.Promise} will store messages
 * between the time they are generated and a {@link me.tatarka.ipromise.Listener} is attached. When
 * the {@code Listener} is attached, all messages in the buffer will be redelivered. You can get
 * some useful defaults from {@link me.tatarka.ipromise.buffer.PromiseBuffers}.
 *
 * @author Evan Tatarka
 */
public interface PromiseBuffer<T> extends Iterable<T> {
    public void add(T item);
}
