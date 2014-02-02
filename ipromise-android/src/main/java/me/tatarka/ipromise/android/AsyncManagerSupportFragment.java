package me.tatarka.ipromise.android;


import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.HashMap;
import java.util.Map;

import me.tatarka.ipromise.Async;

/**
 * @author Evan Tatarka
 */
public class AsyncManagerSupportFragment extends Fragment implements IAsyncManager {
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
    public <T> Async<T> get(String tag) {
        return helper.get(tag);
    }

    @Override
    public <T> void put(String tag, Async<T> async) {
        helper.put(tag, async);
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
