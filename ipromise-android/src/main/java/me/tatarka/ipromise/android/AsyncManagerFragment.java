package me.tatarka.ipromise.android;


import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;

import me.tatarka.ipromise.Promise;

/**
 * Persists the task by running it in a fragment with {@code setRetainInstanceState(true)}. This is
 * used internally by {@link me.tatarka.ipromise.android.AsyncManager}.
 *
 * @author Evan Tatarka
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AsyncManagerFragment extends Fragment implements IAsyncManager {
    private AsyncManagerFragmentHelper helper = new AsyncManagerFragmentHelper();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        helper.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        helper.onDestroy();
    }

    @Override
    public <T> Promise<T> get(String tag) {
        return helper.get(tag);
    }

    @Override
    public <T> void put(String tag, Promise<T> promise) {
        helper.put(tag, promise);
    }

    @Override
    public void cancelAll() {
        helper.cancelAll();
    }

    @Override
    public <T> void save(String tag, SaveCallback<T> callback) {
        helper.save(tag, callback);
    }

    @Override
    public <T> T restore(String tag, SaveCallback<T> callback) {
        return helper.restore(tag, callback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        helper.onSaveInstanceState(outState);
    }
}
