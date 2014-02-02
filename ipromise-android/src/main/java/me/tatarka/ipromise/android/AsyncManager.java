package me.tatarka.ipromise.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

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
    static final String RESULT_TAG = AsyncManager.class.getCanonicalName() + "_result_";

    private final IAsyncManager manager;
    private final Handler handler;

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

        AsyncManagerFragment fragment = (AsyncManagerFragment) activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new AsyncManagerFragment();
            activity.getFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new AsyncManager(fragment);
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
        return new AsyncManager(manager);
    }

    AsyncManager(IAsyncManager manager) {
        this.manager = manager;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public <T> AsyncItem<T> add(String tag, Task<T> task, AsyncCallback<T> callback) {
        return new AsyncItem<T>(handler, manager, tag, task, callback);
    }

    public <T> AsyncItem<T> add(Task<T> task, AsyncCallback<T> callback) {
        return new AsyncItem<T>(handler, manager, DEFAULT, task, callback);
    }

    public <T> AsyncItem<T> start(String tag, Task<T> task, AsyncCallback<T> callback) {
        return new AsyncItem<T>(handler, manager, tag, task, callback).start();
    }

    public <T> AsyncItem<T> start(Task<T> task, AsyncCallback<T> callback) {
        return new AsyncItem<T>(handler, manager, DEFAULT, task, callback).start();
    }

    /**
     * Returns if the {@code AsyncManager} contains a task with the given tag.
     *
     * @param tag the task's tag
     * @return true if {@code AsyncManger} contains the tag, false otherwise
     */
    public boolean contains(String tag) {
        return manager.get(tag) != null;
    }

    /**
     * Cancels all existing tasks
     */
    public void cancelAll() {
        manager.cancelAll();
    }
}
