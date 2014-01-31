package me.tatarka.ipromise;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * User: evantatarka
 * Date: 1/31/14
 * Time: 5:29 PM
 */
public class ProgressExecutorTask<T> implements ProgressTask<T> {
    protected Executor executor;
    protected Do<T> callback;

    public ProgressExecutorTask(Do<T> callback) {
        this(Executors.newSingleThreadExecutor(), callback);
    }

    public ProgressExecutorTask(Executor executor, Do<T> callback) {
        this.executor = executor;
        this.callback = callback;
    }

    @Override
    public Progress<T> start() {
        final CancelToken cancelToken = new CancelToken();
        final Channel<T> channel = new Channel<T>(cancelToken);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                callback.run(channel, cancelToken);
            }
        });
        return channel.progress();
    }
}
