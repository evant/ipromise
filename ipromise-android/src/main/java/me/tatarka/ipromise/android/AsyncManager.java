package me.tatarka.ipromise.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;

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

public class AsyncManager {
    public static final String DEFAULT = AsyncManager.class.getCanonicalName() + "_default";
    private static final String FRAGMENT_TAG = AsyncManager.class.getCanonicalName() + "_fragment";

    private final IAsyncManager manager;
    private final Handler handler;
    private final Map<String, AsyncCallback> callbacks = new HashMap<String, AsyncCallback>();
    private final Map<String, PendingCallback> pendingCallbacks = new HashMap<String, PendingCallback>();

    public static AsyncManager get(FragmentActivity activity) {
        AsyncManagerSupportFragment fragment = (AsyncManagerSupportFragment) activity.getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new AsyncManagerSupportFragment();
            activity.getSupportFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new AsyncManager(fragment);
    }

    public static AsyncManager get(android.support.v4.app.Fragment fragment) {
        AsyncManagerSupportFragment manager = (AsyncManagerSupportFragment) fragment.getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (manager == null) {
            manager = new AsyncManagerSupportFragment();
            fragment.getChildFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new AsyncManager(manager);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static AsyncManager get(Activity activity) {
        AsyncManagerFragment fragment = (AsyncManagerFragment) activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new AsyncManagerFragment();
            activity.getFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new AsyncManager(fragment);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static AsyncManager get(Fragment fragment) {
        AsyncManagerFragment manager = (AsyncManagerFragment) fragment.getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (manager == null) {
            manager = new AsyncManagerFragment();
            fragment.getChildFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new AsyncManager(manager);
    }

    private AsyncManager(IAsyncManager manager) {
        this.manager = manager;
        this.handler = new Handler(Looper.getMainLooper());
    }

    private void put(String tag, Async<?> async) {
        manager.put(tag, async);
    }

    private <T> Async<T> get(String tag) {
        return manager.get(tag);
    }

    public <T> void init(Task<T> task) {
        init(DEFAULT, task);
    }

    public <T> void init(Task<T> task, AsyncCallback<T> callback) {
        init(DEFAULT, task, callback);
    }

    public <T> void init(String tag, Task<T> task, AsyncCallback<T> callback) {
        listen(tag, callback);
        init(tag, task);
    }

    public <T> void init(String tag, Task<T> task) {
        Async<T> async = get(tag);
        if (async == null) {
            async = task.start();
            put(tag, async);
        }

        setupCallback(tag, async);
    }

    public <T> void restart(Task<T> task) {
        restart(DEFAULT, task);
    }

    public <T> void restart(String tag, Task<T> task) {
        Async<T> async = get(tag);
        if (async != null) {
            async.cancelToken().cancel();
        }
        async = task.start();
        put(tag, async);

        setupCallback(tag, async);
    }

    public <T> void listen(AsyncCallback<T> callback) {
        listen(DEFAULT, callback);
    }

    public <T> void listen(String tag, final AsyncCallback<T> callback) {
        if (callbacks.containsKey(tag)) throw new IllegalArgumentException("'" + tag + "' has already been used");
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


    public boolean contains(String tag) {
        return get(tag) != null;
    }

    public boolean cancel(String tag) {
        Async async = get(tag);
        if (async != null) {
            async.cancelToken().cancel();
            return true;
        }
        return false;
    }

    public boolean isRunning(String tag) {
        Async async = get(tag);
        return async != null && async.isRunning();
    }

    public boolean isClosed(String tag) {
        Async async = get(tag);
        return async != null && async instanceof Closeable && ((Closeable) async).isClosed();
    }

    public boolean isCanceled(String tag) {
        Async async = get(tag);
        return async != null && async.cancelToken().isCanceled();
    }

    public void cancelAll() {
        manager.cancelAll();
        callbacks.clear();
        pendingCallbacks.clear();
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
