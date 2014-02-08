package me.tatarka.ipromise.android;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.CloseListener;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Task;

/**
 * Manages a {@link me.tatarka.ipromise.Task} added to an {@link me.tatarka.ipromise.android.AsyncManager}.
 * You can query for the task's state, and start and restart it.
 *
 * @param <T> the result type
 */
public class AsyncItem<T> {
    private Handler handler;
    private IAsyncManager manager;
    private String tag;
    private Task<T> task;
    private AsyncCallback<T> callback;
    private SaveCallback<T> saveCallback;

    private boolean isSetup;
    private boolean isListen;

    AsyncItem(Handler handler, IAsyncManager manager, String tag, Task<T> task, AsyncCallback<T> callback, SaveCallback<T> saveCallback) {
        this.handler = handler;
        this.manager = manager;
        this.tag = tag;
        this.task = task;
        this.callback = callback;
        this.saveCallback = saveCallback;

        if (saveCallback != null) {
            T result = manager.restore(tag, saveCallback);
            if (result != null) callback.receive(result);
        }

        Promise<T> promise = manager.get(tag);
        if (promise != null) {
            setupCallback(promise);
        }
    }

    /**
     * Starts the task. If the task is already started, does nothing.
     *
     * @return the {@code AsyncItem} for chaining
     */
    public AsyncItem<T> start() {
        Promise<T> promise = manager.get(tag);
        if (promise == null) {
            promise = task.start();
            manager.put(tag, promise);
        }

        setupCallback(promise);
        return this;
    }

    /**
     * Restarts the task. If the task is already started, cancels the previously running one.
     *
     * @return the {@code AsyncItem} for chaining
     */
    public AsyncItem<T> restart() {
        Promise<T> promise = manager.get(tag);
        if (promise != null) {
            promise.cancelToken().cancel();
        }
        promise = task.start();
        manager.put(tag, promise);

        isSetup = false;
        setupCallback(promise);
        return this;
    }

    /**
     * Cancels the task.
     *
     * @return true if the task was started, false otherwise
     */
    public boolean cancel() {
        Promise promise = manager.get(tag);
        if (promise != null) {
            promise.cancelToken().cancel();
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
        Promise promise = manager.get(tag);
        return promise != null && promise.isRunning();
    }

    /**
     * Returns if the task is closed. This is only valid for {@link me.tatarka.ipromise.Promise}.
     *
     * @return true if the task is closed, false otherwise
     */
    public boolean isClosed() {
        Promise promise = manager.get(tag);
        return promise != null && promise.isClosed();
    }

    /**
     * Returns if the task is canceled.
     *
     * @return true if the task is canceled, false otherwise
     */
    public boolean isCanceled() {
        Promise promise = manager.get(tag);
        return promise != null && promise.cancelToken().isCanceled();
    }

    private void setupCallback(Promise<T> promise) {
        if (!isSetup) {
            isSetup = true;

            if (saveCallback != null) {
                manager.save(tag, saveCallback);
            }

            if (promise.isRunning()) {
                if (callback != null) callback.start();
            } else if (promise.isClosed()) {
                if (callback != null) callback.end();
            }
        }

        setupListen(promise);
    }

    private void setupListen(Promise<T> promise) {
        if (!isListen) {
            isListen = true;
            setupListen(this, promise);
        }
    }

    private static <T> void setupListen(AsyncItem<T> item, final Promise<T> promise) {
        final WeakReference<AsyncItem<T>> managerRef = new WeakReference<AsyncItem<T>>(item);

        promise.listen(new Listener<T>() {
            @Override
            public void receive(final T result) {
                AsyncItem<T> item = managerRef.get();
                if (item == null) return;

                runOnUI(item.handler, promise, new Runnable() {
                    @Override
                    public void run() {
                        AsyncItem<T> item = managerRef.get();
                        if (item == null || item.callback == null) return;
                        item.callback.receive(result);
                    }
                });
            }
        });

        promise.onClose(new CloseListener() {
            @Override
            public void close() {
                AsyncItem<T> item = managerRef.get();
                if (item == null) return;

                runOnUI(item.handler, promise, new Runnable() {
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

    private static <T> void runOnUI(final Handler handler, Promise<T> promise, final Runnable runnable) {
        // Already on main thread, just call
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            promise.cancelToken().listen(new CancelToken.Listener() {
                @Override
                public void canceled() {
                    handler.removeCallbacks(runnable);
                }
            });
            handler.post(runnable);
        }
    }
}
