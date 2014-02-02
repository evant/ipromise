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
 *
 * @author Evan Tatarka
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

    /**
     * Registers a {@link me.tatarka.ipromise.Task} to the {@code AsyncManager}. This should be
     * called in {@link android.app.Activity#onCreate(android.os.Bundle)} or a similar method. The
     * task will not be started until you call {@link AsyncItem#start()} or {@link
     * AsyncItem#restart()}.
     *
     * @param tag          the task's tag. Each task for this {@code AsyncManager} must have a
     *                     unique tag.
     * @param task         the task
     * @param callback     the callback
     * @param saveCallback the save callback. This will save and restore the result if the Activity
     *                     is destroyed and recreated.
     * @param <T>          the result type
     * @return the {@link me.tatarka.ipromise.android.AsyncItem}
     */
    public <T> AsyncItem<T> add(String tag, Task<T> task, AsyncCallback<T> callback, SaveCallback<T> saveCallback) {
        return new AsyncItem<T>(handler, manager, tag, task, callback, saveCallback);
    }

    /**
     * Registers a {@link me.tatarka.ipromise.Task} to the {@code AsyncManager}. This should be
     * called in {@link android.app.Activity#onCreate(android.os.Bundle)} or a similar method. The
     * task will not be started until you call {@link AsyncItem#start()} or {@link
     * AsyncItem#restart()}.
     *
     * @param task         the task
     * @param callback     the callback
     * @param saveCallback the save callback. This will save and restore the result if the Activity
     *                     is destroyed and recreated.
     * @param <T>          the result type
     * @return the {@link me.tatarka.ipromise.android.AsyncItem}
     */
    public <T> AsyncItem<T> add(Task<T> task, AsyncCallback<T> callback, SaveCallback<T> saveCallback) {
        return new AsyncItem<T>(handler, manager, DEFAULT, task, callback, saveCallback);
    }

    /**
     * Registers a {@link me.tatarka.ipromise.Task} to the {@code AsyncManager}. This should be
     * called in {@link android.app.Activity#onCreate(android.os.Bundle)} or a similar method. The
     * task will not be started until you call {@link AsyncItem#start()} or {@link
     * AsyncItem#restart()}.
     *
     * @param tag      the task's tag. Each task for this {@code AsyncManager} must have a unique
     *                 tag.
     * @param task     the task
     * @param callback the callback
     * @param <T>      the result type
     * @return the {@link me.tatarka.ipromise.android.AsyncItem}
     */
    public <T> AsyncItem<T> add(String tag, Task<T> task, AsyncCallback<T> callback) {
        return new AsyncItem<T>(handler, manager, tag, task, callback, null);
    }

    /**
     * Registers a {@link me.tatarka.ipromise.Task} to the {@code AsyncManager}. This should be
     * called in {@link android.app.Activity#onCreate(android.os.Bundle)} or a similar method. The
     * task will not be started until you call {@link AsyncItem#start()} or {@link
     * AsyncItem#restart()}.
     *
     * @param task     the task
     * @param callback the callback
     * @param <T>      the result type
     * @return the {@link me.tatarka.ipromise.android.AsyncItem}
     */
    public <T> AsyncItem<T> add(Task<T> task, AsyncCallback<T> callback) {
        return new AsyncItem<T>(handler, manager, DEFAULT, task, callback, null);
    }

    /**
     * Registers a {@link me.tatarka.ipromise.Task} to the {@code AsyncManager} and immediately
     * starts it if needed. This should be called in {@link android.app.Activity#onCreate(android.os.Bundle)}
     * or a similar method. This is equivalent to calling {@code add(...).start()}
     *
     * @param tag          the task's tag. Each task for this {@code AsyncManager} must have a
     *                     unique tag.
     * @param task         the task
     * @param callback     the callback
     * @param saveCallback the save callback. This will save and restore the result if the Activity
     *                     is destroyed and recreated.
     * @param <T>          the result type
     * @return the {@link me.tatarka.ipromise.android.AsyncItem}
     */
    public <T> AsyncItem<T> start(String tag, Task<T> task, AsyncCallback<T> callback, SaveCallback<T> saveCallback) {
        return new AsyncItem<T>(handler, manager, tag, task, callback, saveCallback).start();
    }

    /**
     * Registers a {@link me.tatarka.ipromise.Task} to the {@code AsyncManager} and immediately
     * starts it if needed. This should be called in {@link android.app.Activity#onCreate(android.os.Bundle)}
     * or a similar method. This is equivalent to calling {@code add(...).start()}
     *
     * @param task         the task
     * @param callback     the callback
     * @param saveCallback the save callback. This will save and restore the result if the Activity
     *                     is destroyed and recreated.
     * @param <T>          the result type
     * @return the {@link me.tatarka.ipromise.android.AsyncItem}
     */
    public <T> AsyncItem<T> start(Task<T> task, AsyncCallback<T> callback, SaveCallback<T> saveCallback) {
        return new AsyncItem<T>(handler, manager, DEFAULT, task, callback, saveCallback).start();
    }

    /**
     * Registers a {@link me.tatarka.ipromise.Task} to the {@code AsyncManager} and immediately
     * starts it if needed. This should be called in {@link android.app.Activity#onCreate(android.os.Bundle)}
     * or a similar method. This is equivalent to calling {@code add(...).start()}
     *
     * @param tag      the task's tag. Each task for this {@code AsyncManager} must have a unique
     *                 tag.
     * @param task     the task
     * @param callback the callback
     * @param <T>      the result type
     * @return the {@link me.tatarka.ipromise.android.AsyncItem}
     */
    public <T> AsyncItem<T> start(String tag, Task<T> task, AsyncCallback<T> callback) {
        return new AsyncItem<T>(handler, manager, tag, task, callback, null).start();
    }

    /**
     * Registers a {@link me.tatarka.ipromise.Task} to the {@code AsyncManager} and immediately
     * starts it if needed. This should be called in {@link android.app.Activity#onCreate(android.os.Bundle)}
     * or a similar method. This is equivalent to calling {@code add(...).start()}
     *
     * @param task     the task
     * @param callback the callback
     * @param <T>      the result type
     * @return the {@link me.tatarka.ipromise.android.AsyncItem}
     */
    public <T> AsyncItem<T> start(Task<T> task, AsyncCallback<T> callback) {
        return new AsyncItem<T>(handler, manager, DEFAULT, task, callback, null).start();
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
