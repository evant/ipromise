package me.tatarka.ipromise;

/**
 * User: evantatarka
 * Date: 1/31/14
 * Time: 5:31 PM
 */
public interface ProgressTask<T> extends Task<T> {
    Progress<T> start();

    public static interface Do<T>  {
        void run(Channel<T> channel, CancelToken cancelToken);
    }
}
