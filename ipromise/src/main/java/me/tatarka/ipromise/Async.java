package me.tatarka.ipromise;

/**
 * User: evantatarka
 * Date: 1/31/14
 * Time: 4:43 PM
 */
public interface Async<T> {
    Async<T> listen(Listener<T> listener);
    boolean isRunning();
    CancelToken cancelToken();
}
