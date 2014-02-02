package me.tatarka.ipromise.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.tatarka.ipromise.Async;
import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.CloseListener;
import me.tatarka.ipromise.Closeable;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Task;

/**
 * A way to manage asynchronous actions in Android that is much easier to get right than an {@link
 * android.os.AsyncTask} or a {@link android.content.Loader}. It properly handles Activity
 * destruction, configuration changes, and posting back to the UI thread.
 */
public class AsyncManager {
    /**
     * The default tag for an async operation. In many cases an Activity or Fragment only needs to
     * run one async operation so it's unnecessary to use tags to differentiate them. If you omit a
     * tag on a method that take one, this tag is used instead.
     */
    public static final String DEFAULT = AsyncManager.class.getCanonicalName() + "_default";

    static final String FRAGMENT_TAG = AsyncManager.class.getCanonicalName() + "_fragment";

    private final IAsyncManager manager;
    private final Handler handler;
    private final Map<String, AsyncCallback> callbacks = new HashMap<String, AsyncCallback>();
    private final Map<String, PendingCallback> pendingCallbacks = new HashMap<String, PendingCallback>();

    /**
     * Get an instance of the {@code AsyncManager} that is tied to the lifecycle of the given {@link
     * android.app.Activity}.
     *
     * @param activity the activity
     * @return the {@code AsyncManager}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static AsyncManager get(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            throw new UnsupportedOperationException("Method only valid in api 11 and above, use AsyncManagerCompat to support older versions (requires support library)");
        }

        AsyncManagerFragment manager = (AsyncManagerFragment) activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (manager == null) {
            manager = new AsyncManagerFragment();
            activity.getFragmentManager().beginTransaction().add(manager, FRAGMENT_TAG).commit();
        }
        return manager.get();
    }

    /**
     * Get an instance of the {@code AsyncManager} that is tied to the lifecycle of the given {@link
     * android.app.Fragment}.
     *
     * @param fragment the fragment
     * @return the {@code AsyncManager}
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static AsyncManager get(Fragment fragment) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            throw new UnsupportedOperationException("Method only valid in api 17 and above, use AsyncManagerCompat to support older versions (requires support library)");
        }

        AsyncManagerFragment manager = (AsyncManagerFragment) fragment.getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG);

        if (manager == null) {
            manager = new AsyncManagerFragment();
            fragment.getChildFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return manager.get();
    }

    AsyncManager(IAsyncManager manager) {
        this.manager = manager;
        this.handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initializes the given task. The task will be started if it hasn't already, otherwise nothing
     * will happen.
     *
     * @param task the task to start
     */
    public void init(Task<?> task) {
        init(DEFAULT, task);
    }

    /**
     * Initializes the given task and registers the given callback. The task will be started if it
     * hasn't already, otherwise nothing will happen
     *
     * @param task     the task to start
     * @param callback the callback
     * @param <T>      the result type
     */
    public <T> void init(Task<T> task, AsyncCallback<T> callback) {
        init(DEFAULT, task, callback);
    }

    /**
     * Initializes the given task and registers the given callback. The task will be started if it
     * hasn't already, otherwise nothing will happen
     *
     * @param tag      the task's tag
     * @param task     the task to start
     * @param callback the callback
     * @param <T>      the result type
     */
    public <T> void init(String tag, Task<T> task, AsyncCallback<T> callback) {
        listen(tag, callback);
        init(tag, task);
    }

    /**
     * Initializes the given task. The task will be started if it hasn't already, otherwise nothing
     * will happen.
     *
     * @param tag  the task's tag
     * @param task the task to start
     */
    public void init(String tag, Task<?> task) {
        Async<?> async = get(tag);
        if (async == null) {
            async = task.start();
            put(tag, async);
        }

        setupCallback(tag, async);
    }

    /**
     * Restarts the given task. If the task has already been started, the running one will be
     * canceled.
     *
     * @param task the task to start
     */
    public void restart(Task<?> task) {
        restart(DEFAULT, task);
    }

    /**
     * Restarts the given task. If the task has already been started, the running one will be
     * canceled.
     *
     * @param tag  the task's tag
     * @param task the task to start
     */
    public void restart(String tag, Task<?> task) {
        Async<?> async = get(tag);
        if (async != null) {
            async.cancelToken().cancel();
        }
        async = task.start();
        put(tag, async);

        setupCallback(tag, async);
    }

    /**
     * Listens to a task.
     *
     * @param callback the callback
     * @see me.tatarka.ipromise.android.AsyncCallback
     */
    public void listen(AsyncCallback<?> callback) {
        listen(DEFAULT, callback);
    }

    /**
     * Listens to a task.
     *
     * @param tag      the task's tag
     * @param callback the callback
     * @param <T>      result type
     */
    public <T> void listen(String tag, final AsyncCallback<T> callback) {
        if (callbacks.containsKey(tag))
            throw new IllegalArgumentException("'" + tag + "' has already been used");
        callbacks.put(tag, callback);

        PendingCallback<T> pending = pendingCallbacks.remove(tag);
        if (pending != null) {
            if (pending.startCalled) callback.start();
            for (T result : pending.results) callback.receive(result);
            if (pending.endCalled) callback.end();
        } else {
            Async<T> async = get(tag);
            if (async != null) setupCallback(tag, async);
        }
    }

    /**
     * Returns if the {@code AsyncManager} contains a task with the given tag.
     *
     * @param tag the task's tag
     * @return true if {@code AsyncManger} contains the tag, false otherwise
     */
    public boolean contains(String tag) {
        return get(tag) != null;
    }

    /**
     * Cancels the task with the given tag.
     *
     * @param tag the task's tag
     * @return true if the {@code AsyncManager} contains the task and it was canceled, false
     * otherwise.
     */
    public boolean cancel(String tag) {
        Async async = get(tag);
        if (async != null) {
            async.cancelToken().cancel();
            return true;
        }
        return false;
    }

    /**
     * Returns if the task with the given tag is running.
     *
     * @param tag the task's tag
     * @return true if the task is running, false otherwise
     */
    public boolean isRunning(String tag) {
        Async async = get(tag);
        return async != null && async.isRunning();
    }

    /**
     * Returns if the task  with the given tag is closed. This is only valid for {@link
     * me.tatarka.ipromise.Progress}.
     *
     * @param tag the task's tag
     * @return true if the task is closed, false otherwise
     */
    public boolean isClosed(String tag) {
        Async async = get(tag);
        return async != null && async instanceof Closeable && ((Closeable) async).isClosed();
    }

    /**
     * Returns if the task with the given tag is canceled.
     *
     * @param tag the task's tag
     * @return true if the task is canceled, false otherwise
     */
    public boolean isCanceled(String tag) {
        Async async = get(tag);
        return async != null && async.cancelToken().isCanceled();
    }

    /**
     * Cancels all existing tasks
     */
    public void cancelAll() {
        manager.cancelAll();
        callbacks.clear();
        pendingCallbacks.clear();
    }

    private void put(String tag, Async<?> async) {
        manager.put(tag, async);
    }

    private <T> Async<T> get(String tag) {
        return manager.get(tag);
    }

    private <T> AsyncCallback<T> getCallback(String tag) {
        AsyncCallback<T> callback = callbacks.get(tag);
        if (callback == null) {
            callback = new PendingCallback<T>();
            pendingCallbacks.put(tag, (PendingCallback) callback);
        }
        return callback;
    }

    private <T> void setupCallback(String tag, Async<T> async) {
        AsyncCallback<T> callback = getCallback(tag);

        if (async.isRunning()) {
            if (callback != null) callback.start();
        } else if (async instanceof Closeable && ((Closeable) async).isClosed()) {
            if (callback != null) callback.end();
        }

        listenCallback(this, tag, async);
    }

    private static <T> void listenCallback(AsyncManager manager, final String tag, final Async<T> async) {
        final WeakReference<AsyncManager> managerRef = new WeakReference<AsyncManager>(manager);

        async.listen(new Listener<T>() {
            @Override
            public void receive(final T result) {
                AsyncManager manager = managerRef.get();
                if (manager == null) return;

                runOnUI(manager.handler, async, new Runnable() {
                    @Override
                    public void run() {
                        AsyncManager manager = managerRef.get();
                        if (manager == null) return;

                        AsyncCallback<T> callback = manager.getCallback(tag);
                        if (callback != null) callback.receive(result);
                    }
                });
            }
        });

        if (async instanceof Closeable) {
            ((Closeable) async).onClose(new CloseListener() {
                @Override
                public void close() {
                    AsyncManager manager = managerRef.get();
                    if (manager == null) return;

                    runOnUI(manager.handler, async, new Runnable() {
                        @Override
                        public void run() {
                            AsyncManager manager = managerRef.get();
                            if (manager == null) return;

                            AsyncCallback<T> callback = manager.getCallback(tag);
                            if (callback != null) callback.end();
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

    private static class PendingCallback<T> implements AsyncCallback<T> {
        boolean startCalled;
        boolean endCalled;
        List<T> results = new ArrayList<T>();

        @Override
        public void start() {
            startCalled = true;
        }

        @Override
        public void receive(T result) {
            results.add(result);
        }

        @Override
        public void end() {
            endCalled = true;
        }
    }
}
