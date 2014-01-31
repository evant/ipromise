package me.tatarka.ipromise.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import me.tatarka.ipromise.Async;
import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Progress;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Task;

public class AsyncManager {
    public static final String DEFAULT = AsyncManager.class.getCanonicalName() + "_default";
    private static final String FRAGMENT_TAG = AsyncManager.class.getCanonicalName() + "_fragment";

    private final WeakReference<IAsyncManager> manager;
    private final Handler handler;
    private final Map<String, AsyncCallback> callbacks = new HashMap<String, AsyncCallback>();

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
        this.manager = new WeakReference<IAsyncManager>(manager);
        this.handler = new Handler(Looper.getMainLooper());
    }

    private void put(String tag, Async<?> async) {
        IAsyncManager m = manager.get();
        if (m != null) {
            m.put(tag, async);
        } else {
            callbacks.clear();
        }
    }

    private <T> Async<T> get(String tag) {
        IAsyncManager m = manager.get();
        if (m != null) {
            return m.get(tag);
        } else {
            callbacks.clear();
            return null;
        }
    }

    public <T> void init(String tag, Task<T> task) {
        Async<T> async = get(tag);
        if (async == null) {
            async = task.start();
            put(tag, async);
        }

        setupCallback(tag, async);
    }

    public <T> void init(Task<T> task) {
        init(DEFAULT, task);
    }

    public <T> void init(String tag, Task<T> task, AsyncCallback<T> callback) {
        listen(tag, callback);
        init(tag, task);
    }

    public <T> void init(Task<T> task, AsyncCallback<T> callback) {
        init(DEFAULT, task, callback);
    }

    public <T> void restart(String tag, Task<T> task) {
        Async<T> async = get(tag);
        if (async != null) {
            async.cancel();
        }
        async = task.start();
        put(tag, async);

        setupCallback(tag, async);
    }

    public <T> void restart(Task<T> task) {
        restart(DEFAULT, task);
    }

    public <T> void listen(String tag, final AsyncCallback<T> callback) {
        Async<T> async = get(tag);
        callbacks.put(tag, callback);
        if (async != null) {
            setupCallback(tag, async);
        }
    }

    public <T> void listen(AsyncCallback<T> callback) {
        listen(DEFAULT, callback);
    }

    private <T> void setupCallback(String tag, Async<T> async) {
        if (async instanceof Promise) {
            setupPromiseCallback(tag, (Promise<T>) async);
        } else if (async instanceof Progress) {
            setupProgressCallback(tag, (Progress<T>) async);
        }
    }

    private <T> void setupPromiseCallback(final String tag, final Promise<T> promise) {
        if (promise.isRunning()) {
            AsyncCallback<T> callback = callbacks.get(tag);
            if (callback != null) callback.start();
        }

        promise.listen(new Listener<T>() {
            @Override
            public void receive(final T result) {
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        AsyncCallback<T> callback = callbacks.get(tag);
                        if (callback != null) callback.receive(result);
                    }
                };

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
        });
    }

    private <T> void setupProgressCallback(final String tag, final Progress<T> progress) {
        if (progress.isRunning()) {
            AsyncCallback<T> callback = callbacks.get(tag);
            if (callback != null) callback.start();
        } else if (progress.isClosed()) {
            AsyncCallback<T> callback = callbacks.get(tag);
            if (callback != null) callback.end();
        }
    }

    public boolean contains(String tag) {
        return get(tag) != null;
    }

    public boolean cancel(String tag) {
        Async async = get(tag);
        if (async != null) {
            async.cancel();
            return true;
        }
        return false;
    }

    public boolean isRunning(String tag) {
        Async async = get(tag);
        return async != null && async.isRunning();
    }

    public void cancelAll() {
        IAsyncManager m = manager.get();
        if (m != null) {
            m.cancelAll();
        }
        callbacks.clear();
    }
}
