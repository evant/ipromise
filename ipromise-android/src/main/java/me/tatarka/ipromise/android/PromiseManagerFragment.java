package me.tatarka.ipromise.android;


import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

import me.tatarka.ipromise.Promise;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class PromiseManagerFragment extends Fragment implements IPromiseManager {
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
