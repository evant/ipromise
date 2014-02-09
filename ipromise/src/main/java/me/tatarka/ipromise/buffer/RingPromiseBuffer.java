package me.tatarka.ipromise.buffer;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * A {@link me.tatarka.ipromise.buffer.PromiseBuffer} that stores a given number of messages and
 * then starts replacing the oldest ones.
 *
 * @param <T> the message type
 * @author Evan Tatarka
 */
public class RingPromiseBuffer<T> implements PromiseBuffer<T> {
    private int capacity;
    private CircularArrayList<T> buffer;

    /**
     * Creates a buffer with the given capacity. When the buffer is at capacity and another message
     * is sent, it will replace the oldest one.
     *
     * @param capacity the buffer's capacity
     */
    public RingPromiseBuffer(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Invalid capacity: " + capacity + ", must be non-negative");
        }
        this.capacity = capacity;
        if (capacity > 0) {
            buffer = new CircularArrayList<T>(capacity);
        }
    }

    @Override
    public void add(T item) {
        if (capacity == 0) return;
        if (buffer.size() == buffer.capacity()) {
            buffer.remove(0);
        }
        buffer.add(item);
    }

    @Override
    public Iterator<T> iterator() {
        if (capacity > 0) {
            return buffer.iterator();
        } else {
            return new EmptyIterator<T>();
        }
    }

    private static class EmptyIterator<T> implements Iterator<T> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new NoSuchElementException();
        }
    }

    /**
     * If you use this code, please retain this comment block.
     *
     * @author Isak du Preez isak at du-preez dot com www.du-preez.com
     */
    private static class CircularArrayList<E> extends AbstractList<E> implements RandomAccess {

        private final int n; // buffer length
        private final List<E> buf; // a List implementing RandomAccess
        private int head = 0;
        private int tail = 0;

        public CircularArrayList(int capacity) {
            n = capacity + 1;
            buf = new ArrayList<E>(Collections.nCopies(n, (E) null));
        }

        public int capacity() {
            return n - 1;
        }

        private int wrapIndex(int i) {
            int m = i % n;
            if (m < 0) { // java modulus can be negative
                m += n;
            }
            return m;
        }

        // This method is O(n) but will never be called if the
        // CircularArrayList is used in its typical/intended role.
        private void shiftBlock(int startIndex, int endIndex) {
            assert (endIndex > startIndex);
            for (int i = endIndex - 1; i >= startIndex; i--) {
                set(i + 1, get(i));
            }
        }

        @Override
        public int size() {
            return tail - head + (tail < head ? n : 0);
        }

        @Override
        public E get(int i) {
            if (i < 0 || i >= size()) {
                throw new IndexOutOfBoundsException();
            }
            return buf.get(wrapIndex(head + i));
        }

        @Override
        public E set(int i, E e) {
            if (i < 0 || i >= size()) {
                throw new IndexOutOfBoundsException();
            }
            return buf.set(wrapIndex(head + i), e);
        }

        @Override
        public void add(int i, E e) {
            int s = size();
            if (s == n - 1) {
                throw new IllegalStateException("Cannot add element."
                        + " CircularArrayList is filled to capacity.");
            }
            if (i < 0 || i > s) {
                throw new IndexOutOfBoundsException();
            }
            tail = wrapIndex(tail + 1);
            if (i < s) {
                shiftBlock(i, s);
            }
            set(i, e);
        }

        @Override
        public E remove(int i) {
            int s = size();
            if (i < 0 || i >= s) {
                throw new IndexOutOfBoundsException();
            }
            E e = get(i);
            set(i, null);
            if (i > 0) {
                shiftBlock(0, i);
            }
            head = wrapIndex(head + 1);
            return e;
        }
    }
}
