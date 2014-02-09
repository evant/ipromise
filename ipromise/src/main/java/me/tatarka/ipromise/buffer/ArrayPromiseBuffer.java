package me.tatarka.ipromise.buffer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link me.tatarka.ipromise.buffer.PromiseBuffer} with an optional fixed capacity. If a capacity
 * is given, an exception will be thrown if too many messages are sent. You should only use this if
 * you know and want to enforce the number of messages sent. Giving no capacity will cause the
 * buffer to be unbounded. Be careful when using it this way as all messages will be kept in memory
 * until the {@link me.tatarka.ipromise.Promise} is garbage collected.
 *
 * @param <T> the type in the buffer
 * @author Evan Tatarka
 */
public class ArrayPromiseBuffer<T> implements PromiseBuffer<T> {
    private int capacity;
    private List<T> buffer;

    /**
     * Constructs a new unbounded {@link me.tatarka.ipromise.buffer.PromiseBuffer}.
     */
    public ArrayPromiseBuffer() {
        this.capacity = -1;
        buffer = new ArrayList<T>();
    }

    /**
     * Constructs a new {@link me.tatarka.ipromise.buffer.PromiseBuffer} bounded at the given
     * capacity. Exceeding that capacity will cause an {@link java.lang.IllegalStateException} to be
     * thrown.
     *
     * @param capacity the max buffer capacity (must be non-zero)
     */
    public ArrayPromiseBuffer(int capacity) {
        if (capacity < 0)
            throw new IllegalArgumentException("Invalid capacity: " + capacity + ", must be non-negative");
        this.capacity = capacity;
        buffer = new ArrayList<T>(capacity);
    }

    @Override
    public void add(T item) {
        if (capacity > 0 && buffer.size() + 1 > capacity)
            throw new IllegalStateException("PromiseBuffers has exceeded it's maximum capacity of " + capacity + " items");
        buffer.add(item);
    }

    @Override
    public Iterator<T> iterator() {
        return buffer.iterator();
    }
}
