package me.tatarka.ipromise.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class AndroidPromiseExecutors {
    private AndroidPromiseExecutors() {
    }

    public static Executor mainLooperCallbackExecutor() {
        return looperCallbackExecutor(Looper.getMainLooper());
    }

    public static Executor looperCallbackExecutor(Looper looper) {
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
