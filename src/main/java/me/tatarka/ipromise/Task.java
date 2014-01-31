package me.tatarka.ipromise;

/**
 * An easy way to start run something asynchronously and return a {@link
 * me.tatarka.ipromise.Promise}.
 *
 * @param <T> the result type
 */
public interface Task<T> {
    Promise<T> start();

    /**
     * The callback for the task to execute
     *
     * @param <T> the result type
     */
    public static interface Do<T> {
        T run(CancelToken cancelToken);
    }

    /**
     * If your callback may throw an exception, you can use this to automatically, catch the
     * exception and return a {@link me.tatarka.ipromise.Result}.
     *
     * @param <T> the success type
     * @param <E> the error type
     */
    public static abstract class DoFailable<T, E extends Exception> implements Do<Result<T, E>> {
        @Override
        public final Result<T, E> run(CancelToken cancelToken) {
            try {
                return Result.success(runFailable(cancelToken));
            } catch (Exception e) {
                return Result.error((E) e);
            }
        }

        public abstract T runFailable(CancelToken cancelToken) throws E;
    }
}
