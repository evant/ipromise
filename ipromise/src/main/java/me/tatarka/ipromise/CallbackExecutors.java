package me.tatarka.ipromise;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * <p> The callback executor used to run the {@link Promise#listen(Listener)} and {@link
 * Promise#onClose(CloseListener)} callbacks. The default is to run callbacks on a single background
 * thread. This is important to give consistency in what execution context the callbacks are run in.
 * It can also be uses to simplify you threading model, e.x. you could post all callbacks to the UI
 * thread. If you want more information on why this is a good idea, see <a
 * href="http://blog.ometer.com/2011/07/24/callbacks-synchronous-and-asynchronous/">this blog
 * post</a>. </p>
 */
public class CallbackExecutors {
    protected CallbackExecutors() {

    }

    private static Executor defaultExecutor;

    /**
     * Sets the default callback executor.
     *
     * <p> <b>Important:</b> The given executor must preserve the order of tasks given to it for a
     * given {@code Promise}. Otherwise you will run into unexpected behavior like messages arriving
     * out of order and your promise closing before all messages arrive. </p>
     *
     * <p> You should only set this once for your application (though not enforced). If you want
     * more granular control, use {@link me.tatarka.ipromise.Deferred.Builder#callbackExecutor(java.util.concurrent.Executor)}
     * or {@link Deferred#Deferred(me.tatarka.ipromise.buffer.PromiseBuffer, CancelToken,
     * java.util.concurrent.Executor)}. </p>
     *
     * @param executor the executor.
     */
    public static void setDefault(Executor executor) {
        defaultExecutor = executor;
    }

    /**
     * Returns the default callback executor. If none is set, the default is to run callbacks on a
     * single background thread.
     *
     * @return the callback executor
     */
    public static Executor getDefault() {
        if (defaultExecutor == null) defaultExecutor = backgroundThreadExecutor();
        return defaultExecutor;
    }

    private static Executor backgroundThreadExecutor;

    /**
     * Returns the the default callback executor that executors all callbacks in a single background
     * thread.
     *
     * @return the default callback executor
     */
    public static Executor backgroundThreadExecutor() {
        if (backgroundThreadExecutor == null) {
            backgroundThreadExecutor = Executors.newSingleThreadExecutor();
        }
        return backgroundThreadExecutor;
    }

    private static Executor sameThreadExecutor;

    /**
     * Returns an executor that runs all callbacks in the same thread that they were sent from.
     * While this is not a good idea in you application code, it is nice to set this in unit tests
     * so that you can have the callback run immediately and not have to worry about synchronizing
     * threads.
     *
     * @return the callback executor
     */
    public static Executor sameThreadExecutor() {
        if (sameThreadExecutor == null) {
            sameThreadExecutor = new Executor() {
                @Override
                public void execute(Runnable command) {
                    command.run();
                }
            };
        }
        return sameThreadExecutor;
    }
}
