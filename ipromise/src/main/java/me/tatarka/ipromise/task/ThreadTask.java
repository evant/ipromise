package me.tatarka.ipromise.task;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Promise;

/**
 * A {@link me.tatarka.ipromise.task.Task} that executes using a single {@link java.lang.Thread}. If
 * the {@link me.tatarka.ipromise.Promise} is canceled, {@link Thread#interrupt()} is called. This
 * is particularly useful for IO operations.
 *
 * @author Evan Tatarka
 */
public class ThreadTask<T> implements Task<T> {
    private Deferred.Builder deferredBuilder;
    private Do<T> callback;

    /**
     * Creates a new {@code Task} that runs the given callback in a new {@link java.lang.Thread}.
     *
     * @param callback the callback
     */
    public ThreadTask(Do<T> callback) {
        this(new Deferred.Builder(), callback);
    }

    /**
     * Creates a new {@code Task} that runs the given callback in a new {@link java.lang.Thread}
     * using the given {@link me.tatarka.ipromise.Deferred.Builder}
     *
     * @param deferredBuilder the deferred builder
     * @param callback        the callback
     */
    public ThreadTask(Deferred.Builder deferredBuilder, Do<T> callback) {
        this.deferredBuilder = deferredBuilder;
        this.callback = callback;
    }

    @Override
    public Promise<T> start() {
        final CancelToken cancelToken = new CancelToken();
        final Deferred<T> deferred = deferredBuilder.build(cancelToken);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                callback.run(deferred, cancelToken);
            }
        });
        cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                thread.interrupt();
            }
        });
        return deferred.promise();
    }
}
