package me.tatarka.ipromise;

public final class PromiseBuffers {
    private PromiseBuffers() { }

    public static PromiseBufferFactory none() {
        return new PromiseBufferFactory() {
            @Override
            public <T> PromiseBuffer<T> create() {
                return new RingPromiseBuffer<T>(0);
            }
        };
    }

    public static PromiseBufferFactory last() {
        return new PromiseBufferFactory() {
            @Override
            public <T> PromiseBuffer<T> create() {
                return new RingPromiseBuffer<T>(1);
            }
        };
    }

    public static PromiseBufferFactory all() {
        return new PromiseBufferFactory() {
            @Override
            public <T> PromiseBuffer<T> create() {
                return new ArrayPromiseBuffer<T>(-1);
            }
        };
    }

    public static <T> PromiseBufferFactory ring(final int size) {
        return new PromiseBufferFactory() {
            @Override
            public <T> PromiseBuffer<T> create() {
                return new RingPromiseBuffer<T>(size);
            }
        };
    }

    public static <T> PromiseBufferFactory fixed(final int size) {
        return new PromiseBufferFactory() {
            @Override
            public <T> PromiseBuffer<T> create() {
                return new ArrayPromiseBuffer<T>(size);
            }
        };
    }

    public static PromiseBufferFactory ofType(int type) {
        switch (type) {
            case Promise.BUFFER_NONE: return none();
            case Promise.BUFFER_LAST: return last();
            case Promise.BUFFER_ALL: return all();
            default:
                throw new IllegalArgumentException("Unknown promise buffer type: " + type);
        }
    }

}
