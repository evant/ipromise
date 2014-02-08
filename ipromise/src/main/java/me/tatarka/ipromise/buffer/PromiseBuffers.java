package me.tatarka.ipromise.buffer;

import me.tatarka.ipromise.Promise;

public final class PromiseBuffers {
    private PromiseBuffers() { }

    public static <T> PromiseBuffer<T> none() {
        return new RingPromiseBuffer<T>(0);
    }

    public static <T> PromiseBuffer<T> last() {
        return new RingPromiseBuffer<T>(1);
    }

    public static <T> PromiseBuffer<T> all() {
        return new ArrayPromiseBuffer<T>(-1);
    }

    public static <T> PromiseBuffer<T> ring(final int size) {
        return new RingPromiseBuffer<T>(size);
    }

    public static <T> PromiseBuffer<T> fixed(final int size) {
        return new ArrayPromiseBuffer<T>(size);
    }

    public static <T> PromiseBuffer<T> ofType(int type) {
        switch (type) {
            case Promise.BUFFER_NONE: return none();
            case Promise.BUFFER_LAST: return last();
            case Promise.BUFFER_ALL: return all();
            default:
                throw new IllegalArgumentException("Unknown promise buffer type: " + type);
        }
    }

}
