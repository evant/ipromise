package me.tatarka.ipromise.android;


import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

import me.tatarka.ipromise.Async;

/**
 * @author Evan Tatarka
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AsyncManagerFragment extends Fragment implements IAsyncManager {
    private AsyncManager asyncManager;
    private Map<String, Async> async = new HashMap<String, Async>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAll();
        async.clear();
    }

    @Override
    public AsyncManager get() {
        if (asyncManager == null)  asyncManager = new AsyncManager(this);
        return asyncManager;
    }

    @Override
    public <T> Async<T> get(String tag) {
        return async.get(tag);
    }

    @Override
    public void put(String tag, Async<?> async) {
        this.async.put(tag, async);
    }

    @Override
    public void cancelAll() {
        for (Async async : this.async.values()) {
            async.cancelToken().cancel();
        }
    }
}
