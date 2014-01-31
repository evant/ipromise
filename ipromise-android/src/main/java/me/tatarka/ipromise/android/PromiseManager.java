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

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Task;

public class PromiseManager {
    public static final String DEFAULT = PromiseManager.class.getCanonicalName() + "_default";
    private static final String FRAGMENT_TAG = PromiseManager.class.getCanonicalName() + "_fragment";

    private final WeakReference<IPromiseManager> manager;
    private final Handler handler;
    private final Map<String, PromiseCallback> callbacks = new HashMap<String, PromiseCallback>();
    private boolean defaultUsed;

    public static PromiseManager get(FragmentActivity activity) {
        PromiseManagerSupportFragment fragment = (PromiseManagerSupportFragment) activity.getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new PromiseManagerSupportFragment();
            activity.getSupportFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new PromiseManager(fragment);
    }

    public static PromiseManager get(android.support.v4.app.Fragment fragment) {
        PromiseManagerSupportFragment manager = (PromiseManagerSupportFragment) fragment.getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (manager == null) {
            manager = new PromiseManagerSupportFragment();
            fragment.getChildFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new PromiseManager(manager);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static PromiseManager get(Activity activity) {
        PromiseManagerFragment fragment = (PromiseManagerFragment) activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new PromiseManagerFragment();
            activity.getFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new PromiseManager(fragment);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static PromiseManager get(Fragment fragment) {
        PromiseManagerFragment manager = (PromiseManagerFragment) fragment.getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (manager == null) {
            manager = new PromiseManagerFragment();
            fragment.getChildFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new PromiseManager(manager);
    }

    private PromiseManager(IPromiseManager manager) {
        this.manager = new WeakReference<IPromiseManager>(manager);
        this.handler = new Handler(Looper.getMainLooper());
    }

    private void put(String tag, Promise<?> promise) {
        IPromiseManager m = manager.get();
        if (m != null) {
            m.put(tag, promise);
        } else {
            callbacks.clear();
        }
    }

    private <T> Promise<T> get(String tag) {
        IPromiseManager m = manager.get();
        if (m != null) {
            return m.get(tag);
        } else {
            callbacks.clear();
            return null;
        }
    }

    public <T> void init(String tag, Task<T> task) {
        Promise<T> promise = get(tag);
        if (promise == null) {
            promise = task.start();
            put(tag, promise);
        }
        setupCallback(tag, promise);
    }

    public <T> void init(Task<T> task) {
        init(DEFAULT, task);
    }

    public <T> void init(String tag, Task<T> task, PromiseCallback<T> callback) {
        listen(tag, callback);
        init(tag, task);
    }

    public <T> void init(Task<T> task, PromiseCallback<T> callback) {
        init(DEFAULT, task, callback);
    }

    public <T> void restart(String tag, Task<T> task) {
        Promise<T> promise = get(tag);
        if (promise != null) {
            promise.cancel();
        }
        promise = task.start();
        put(tag, promise);
        setupCallback(tag, promise);
    }

    public <T> void restart(Task<T> task) {
        restart(DEFAULT, task);
    }

    public <T> void listen(String tag, final PromiseCallback<T> callback) {
        Promise<T> promise = get(tag);
        callbacks.put(tag, callback);
        if (promise != null) {
            setupCallback(tag, promise);
        }
    }

    public <T> void listen(PromiseCallback<T> callback) {
        listen(DEFAULT, callback);
    }

    private <T> void setupCallback(final String tag, final Promise<T> promise) {
        if (!(promise.isFinished() || promise.isCanceled())) {
            PromiseCallback<T> callback = callbacks.get(tag);
            if (callback != null) callback.start();
        }

        promise.listen(new Listener<T>() {
            @Override
            public void receive(final T result) {
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        PromiseCallback<T> callback = callbacks.get(tag);
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

    public boolean contains(String tag) {
        return get(tag) != null;
    }

    public boolean cancel(String tag) {
        Promise promise = get(tag);
        if (promise != null) {
            promise.cancel();
            return true;
        }
        return false;
    }

    public boolean isRunning(String tag) {
        Promise promise = get(tag);
        return promise != null && !(promise.isFinished() || promise.isCanceled());
    }

    public void cancelAll() {
        IPromiseManager m = manager.get();
        if (m != null) {
            m.cancelAll();
        }
        callbacks.clear();
    }
}
