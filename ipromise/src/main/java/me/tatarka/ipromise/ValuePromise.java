package me.tatarka.ipromise;

import java.util.concurrent.Executor;

import me.tatarka.ipromise.buffer.PromiseBuffer;

class ValuePromise<T> extends Promise<T> {
    private PromiseBuffer<T> buffer;

    ValuePromise(PromiseBuffer<T> buffer, CancelToken cancelToken, Executor callbackExecutor) {
        super(cancelToken, callbackExecutor);
        this.buffer = buffer;
        cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                listeners.clear();
            }
        });
    }

    @Override
    protected void onSend(T message) {
        buffer.add(message);
    }

    @Override
    protected void onListen(Listener<T> listener) {
        for (T message : buffer) {
            dispatch(callbackExecutor, listener, message);
        }
    }
}
