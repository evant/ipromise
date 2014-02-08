package me.tatarka.ipromise;

import me.tatarka.ipromise.buffer.ArrayPromiseBuffer;
import me.tatarka.ipromise.buffer.PromiseBuffer;

public class ValuePromise<T> extends Promise<T> {
    private PromiseBuffer<T> buffer;

    /**
     * Constructs a new promise with the given {@link CancelToken}. When the token is canceled, the
     * promise is also canceled. This is used internally by {@link Deferred}.
     *
     * @param cancelToken the cancel token
     */
    ValuePromise(PromiseBuffer<T> buffer, CancelToken cancelToken) {
        super(cancelToken);
        this.buffer = buffer;
        cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                listeners.clear();
            }
        });
    }

    /**
     * Constructs a promise with a result already in it. This is useful for when you can return the
     * value immediately.
     *
     * @param result the result
     */
    public ValuePromise(T result) {
        super(new CancelToken());
        buffer = new ArrayPromiseBuffer<T>(1);
        buffer.add(result);
        close();
    }

    @Override
    protected void onSend(T message) {
        buffer.add(message);
    }

    @Override
    protected void onListen(Listener<T> listener) {
        for (T message : buffer) {
            dispatch(message);
        }
    }
}
