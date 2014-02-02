package me.tatarka.ipromise.android;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

import me.tatarka.ipromise.Async;
import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.CloseListener;
import me.tatarka.ipromise.Closeable;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Task;

public class AsyncItem<T> {
    private Handler handler;
    private IAsyncManager manager;
    private String tag;
    private Task<T> task;
    private AsyncCallback<T> callback;

    private boolean isSetup;
    private boolean isListen;

    AsyncItem(Handler handler, IAsyncManager manager, String tag, Task<T> task, AsyncCallback<T> callback) {
        this.handler = handler;
        this.manager = manager;
        this.tag = tag;
        this.task = task;
        this.callback = callback;

        Async<T> async = manager.get(tag);
        if (async != null) {
            setupCallback(async);
        }
    }

    public AsyncItem<T> start() {
        Async<T> async = manager.get(tag);
        if (async == null) {
            async = task.start();
            manager.put(tag, async);
        }

        setupCallback(async);
        return this;
    }

    public AsyncItem<T> restart() {
        Async<T> async = manager.get(tag);
        if (async != null) {
            async.cancelToken().cancel();
        }
        async = task.start();
        manager.put(tag, async);

        isSetup = false;
        setupCallback(async);
        return this;
    }

    public boolean cancel() {
        Async async = manager.get(tag);
        if (async != null) {
            async.cancelToken().cancel();
            return true;
        }
        return false;
    }

    /**
     * Returns if the task is running.
     *
     * @return true if the task is running, false otherwise
     */
    public boolean isRunning() {
        Async async = manager.get(tag);
        return async != null && async.isRunning();
    }

    /**
     * Returns if the task is closed. This is only valid for {@link me.tatarka.ipromise.Progress}.
     *
     * @return true if the task is closed, false otherwise
     */
    public boolean isClosed() {
        Async async = manager.get(tag);
        return async != null && async instanceof Closeable && ((Closeable) async).isClosed();
    }

    /**
     * Returns if the task is canceled.
     *
     * @return true if the task is canceled, false otherwise
     */
    public boolean isCanceled() {
        Async async = manager.get(tag);
        return async != null && async.cancelToken().isCanceled();
    }

    private void setupCallback(Async<T> async) {
        if (!isSetup) {
            isSetup = true;
            if (async.isRunning()) {
                if (callback != null) callback.start();
            } else if (async instanceof Closeable && ((Closeable) async).isClosed()) {
                if (callback != null) callback.end();
            }
        }

        setupListen(async);
    }

    private void setupListen(Async<T> async) {
        if (!isListen) {
            isListen = true;
            setupListen(this, async);
        }
    }

    private static <T> void setupListen(AsyncItem<T> item, final Async<T> async) {
        final WeakReference<AsyncItem<T>> managerRef = new WeakReference<AsyncItem<T>>(item);

        async.listen(new Listener<T>() {
            @Override
            public void receive(final T result) {
                AsyncItem<T> item = managerRef.get();
                if (item == null) return;

                runOnUI(item.handler, async, new Runnable() {
                    @Override
                    public void run() {
                        AsyncItem<T> item = managerRef.get();
                        if (item == null || item.callback == null) return;
                        item.callback.receive(result);
                    }
                });
            }
        });

        if (async instanceof Closeable) {
            ((Closeable) async).onClose(new CloseListener() {
                @Override
                public void close() {
                    AsyncItem<T> item = managerRef.get();
                    if (item == null) return;

                    runOnUI(item.handler, async, new Runnable() {
                        @Override
                        public void run() {
                            AsyncItem<T> item = managerRef.get();
                            if (item == null || item.callback == null) return;
                            item.callback.end();
                        }
                    });
                }
            });
        }
    }

    private static <T> void runOnUI(final Handler handler, Async<T> async, final Runnable runnable) {
        // Already on main thread, just call
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            async.cancelToken().listen(new CancelToken.Listener() {
                @Override
                public void canceled() {
                    handler.removeCallbacks(runnable);
                }
            });
            handler.post(runnable);
        }
    }
}
