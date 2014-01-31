package me.tatarka.ipromise.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;

import java.util.HashMap;
import java.util.Map;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Task;

public class PromiseManager {
    private static final String FRAGMENT_TAG = PromiseManager.class.getCanonicalName() + "_fragment";

    private final IPromiseManager manager;
    private final Handler handler;
    private final Map<String, PromiseCallback> callbacks = new HashMap<String, PromiseCallback>();

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
        this.manager = manager;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public <T> void init(String tag, Task<T> task) {
        Promise<T> promise = manager.get(tag);
        if (promise == null) {
            promise = task.start();
            manager.put(tag, promise);
        }
        setupCallback(tag, promise);
    }

    public <T> void init(String tag, Task<T> task, PromiseCallback<T> callback) {
        init(tag, task);
        listen(tag, callback);
    }

    public <T> void restart(String tag, Task<T> task) {
        Promise<T> promise = manager.get(tag);
        if (promise != null) {
            promise.cancel();
        }
        promise = task.start();
        manager.put(tag, promise);
        setupCallback(tag, promise);
    }

    public <T> void restart(String tag, Task<T> task, PromiseCallback<T> callback) {
        restart(tag, task);
        listen(tag, callback);
    }

    public <T> void listen(String tag, final PromiseCallback<T> callback) {
        Promise<T> promise = manager.get(tag);
        callbacks.put(tag, callback);
        if (promise != null) {
            setupCallback(tag, promise);
        }
    }

    private <T> void setupCallback(final String tag, final Promise<T> promise) {
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
        return manager.get(tag) != null;
    }

    public boolean cancel(String tag) {
        Promise promise = manager.get(tag);
        if (promise != null) {
            promise.cancel();
            return true;
        }
        return false;
    }

    public boolean isRunning(String tag) {
        Promise promise = manager.get(tag);
        return promise != null && !(promise.isFinished() || promise.isCanceled());
    }

    public void cancelAll() {
        manager.cancelAll();
    }
}
