package me.tatarka.ipromise.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

import me.tatarka.ipromise.CallbackExecutors;

/**
 * Callback executors that take advantage of Android's event loop. It is a good idea to set one of
 * these as the default so a separate thread does not have to be created.
 *
 * @see me.tatarka.ipromise.CallbackExecutors
 */
public class AndroidCallbackExecutors extends CallbackExecutors {
    protected AndroidCallbackExecutors() {
    }

    private static Executor mainLooperExecutor;

    /**
     * Returns an executor that post all callbacks to the UI thread. In most cases you should set
     * this one to default since you will be probably updating the UI in callbacks anyway.
     *
     * @return the callback executor
     */
    public static Executor mainLooperExecutor() {
        if (mainLooperExecutor == null) {
            mainLooperExecutor = looperExecutor(Looper.getMainLooper());
        }
        return mainLooperExecutor;
    }

    /**
     * Returns an executor that post all callbacks to the given looper.
     *
     * @param looper the looper to post callbacks to
     * @return the callback executor
     */
    public static Executor looperExecutor(Looper looper) {
        return new LooperExecutor(looper);
    }

    private static class LooperExecutor implements Executor {
        private Handler handler;

        public LooperExecutor(Looper looper) {
            handler = new Handler(looper);
        }

        @Override
        public void execute(Runnable runnable) {
            handler.post(runnable);
        }
    }
}
