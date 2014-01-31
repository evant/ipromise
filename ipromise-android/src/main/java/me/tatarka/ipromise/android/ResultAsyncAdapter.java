package me.tatarka.ipromise.android;

import me.tatarka.ipromise.Result;

/**
 * User: evantatarka
 * Date: 1/31/14
 * Time: 4:38 PM
 */
public abstract class ResultAsyncAdapter<T, E extends Exception> extends AsyncAdapter<Result<T, E>> {
    @Override
    public void receive(Result<T, E> result) {
        if (result.isSuccess()) {
            success(result.getSuccess());
        } else {
            error(result.getError());
        }
    }

    public abstract void success(T success);
    public abstract void error(E error);
}
