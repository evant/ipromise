package me.tatarka.ipromise.android;


import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.HashMap;
import java.util.Map;

import me.tatarka.ipromise.Promise;

public class PromiseManagerSupportFragment extends Fragment implements IPromiseManager {
    private Map<String, Promise> promises = new HashMap<String, Promise>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAll();
        promises.clear();
    }

    @Override
    public <T> Promise<T> get(String tag) {
        return promises.get(tag);
    }

    @Override
    public void put(String tag, Promise<?> promise) {
        promises.put(tag, promise);

    }

    @Override
    public void cancelAll() {
        for (Promise promise : promises.values()) {
            promise.cancel();
        }
    }
}