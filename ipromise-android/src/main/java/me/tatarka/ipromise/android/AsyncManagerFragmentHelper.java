package me.tatarka.ipromise.android;

import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import me.tatarka.ipromise.Async;
import me.tatarka.ipromise.Listener;

class AsyncManagerFragmentHelper implements IAsyncManager {
    private Map<String, Async> async = new HashMap<String, Async>();
    private Map<String, SaveItem> savedResults = new HashMap<String, SaveItem>();
    private Bundle savedState;

    public void onCreate(Bundle savedState) {
        this.savedState = savedState;
    }

    public void onDestroy() {
        cancelAll();
        async.clear();
    }

    @Override
    public <T> Async<T> get(String tag) {
        return async.get(tag);
    }

    @Override
    public <T> void put(String tag, Async<T> async) {
        this.async.put(tag, async);
    }

    @Override
    public void cancelAll() {
        for (Async async : this.async.values()) {
            async.cancelToken().cancel();
        }
    }

    @Override
    public <T> void save(String tag, SaveCallback<T> callback) {
        SaveItem saveItem = savedResults.get(tag);
        if (saveItem == null) {
            saveItem = new SaveItem();
            savedResults.put(tag, saveItem);
            setupSaveListener(tag, saveItem);
        }
        saveItem.saveCallback = new WeakReference<SaveCallback>(callback);
    }

    @Override
    public <T> T restore(String tag, SaveCallback<T> callback) {
        if (savedState != null) {
            T result = callback.asyncRestore(savedState);
            if (result != null) {
                SaveItem item = new SaveItem();
                item.result = result;
                item.saveCallback = new WeakReference<SaveCallback>(callback);
                savedResults.put(tag, item);
            }
            return result;
        }
        return null;
    }

    public void onSaveInstanceState(Bundle outState) {
        for (SaveItem saveItem : savedResults.values()) {
            SaveCallback callback = saveItem.saveCallback.get();
            if (callback == null) continue;
            callback.asyncSave(saveItem.result, outState);
        }
    }

    private void setupSaveListener(String tag, final SaveItem saveItem) {
        Async async = get(tag);
        async.listen(new Listener() {
            @Override
            public void receive(Object result) {
                saveItem.result = result;
            }
        });
    }

    private static class SaveItem {
        Object result;
        WeakReference<SaveCallback> saveCallback;
    }
}
