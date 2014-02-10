package me.tatarka.ipromise.task;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Result;

/**
 * A way to control an async operation.
 *
 * @param <T> the result type
 * @author Evan Tatarka
 */
public interface Task<T> {
    /**
     * Starts the async operation. This method is expected to return immediately, running the
     * operation asynchronously.
     *
     * @return an {@link me.tatarka.ipromise.Promise} to manage the operation's result.
     */
    Promise<T> start();

    /**
     * A callback for delivering messages to a {@link me.tatarka.ipromise.Deferred}. A implementer
     * of {@code Task} may accept this and run it in it's own context.
     *
     * @param <T> the message type
     */
    public interface Do<T> {
        void run(Deferred<T> deferred, CancelToken cancelToken);
    }

    /**
     * You can subclass this instead of implementing {@link me.tatarka.ipromise.task.Task.Do} if you
     * only have one message to send.
     *
     * @param <T> the message type
     */
    public static abstract class DoOnce<T> implements Do<T> {
        @Override
        public final void run(Deferred<T> deferred, CancelToken cancelToken) {
            deferred.resolve(runOnce(cancelToken));
        }

        public abstract T runOnce(CancelToken cancelToken);
    }

    /**
     * You can subclass this instead of implementing {@link me.tatarka.ipromise.task.Task.Do} if you
     * only have one message to send that may throw an exception. The message is wrapped in a {@link
     * me.tatarka.ipromise.Result} that may be either your return value or your exception.
     *
     * @param <T> the success result type
     * @param <E> the failure result type
     */
    public static abstract class DoOnceFailable<T, E extends Exception> implements Do<Result<T, E>> {
        @Override
        public final void run(Deferred<Result<T, E>> deferred, CancelToken cancelToken) {
            try {
                deferred.resolve(Result.<T, E>success(runFailable(cancelToken)));
            } catch (Exception e) {
                deferred.resolve(Result.<T, E>error((E) e));
            }
        }

        public abstract T runFailable(CancelToken cancelToken) throws E;
    }

    /**
     * You can subclass this instead of implementing {@link me.tatarka.ipromise.task.Task.Do} if you
     * want to send multiple messages. The task will automatically close the {@link
     * me.tatarka.ipromise.Deferred}.
     *
     * @param <T> the message type
     */
    public static abstract class DoMany<T> implements Do<T> {
        @Override
        public final void run(Deferred<T> deferred, CancelToken cancelToken) {
            try {
                Sender<T> sender = new DeferredSender<T>(deferred);
                runMay(sender, cancelToken);
            } finally {
                deferred.close();
            }
        }

        public abstract void runMay(Sender<T> sender, CancelToken cancelToken);

        private static class DeferredSender<T> implements Sender<T> {
            private Deferred<T> deferred;

            DeferredSender(Deferred<T> deferred) {
                this.deferred = deferred;
            }

            @Override
            public Sender<T> send(T message) {
                deferred.send(message);
                return this;
            }

            @Override
            public Sender<T> sendAll(Iterable<T> messages) {
                deferred.sendAll(messages);
                return this;
            }

            @Override
            public Sender<T> sendAll(T... messages) {
                deferred.sendAll(messages);
                return this;
            }
        }
    }

    /**
     * You can subclass this instead of implementing {@link me.tatarka.ipromise.task.Task.Do} if you
     * want to send multiple messages and may fail. The messages are wrapped in a {@link
     * me.tatarka.ipromise.Result} that may either be a message you send or your exception if
     * thrown. When an exception is thrown the {@link me.tatarka.ipromise.Deferred} will send a
     * final {@link me.tatarka.ipromise.Result#error(Exception)} and then will be closed. The Task
     * will automatically close the {@code Deferred}.
     *
     * @param <T> the message type
     * @param <E> the exception type
     */
    public static abstract class DoManyFailable<T, E extends Exception> implements Do<Result<T, E>> {
        @Override
        public final void run(Deferred<Result<T, E>> deferred, CancelToken cancelToken) {
            try {
                Sender<T> sender = new FailableDeferredSender<T, E>(deferred);
                runMayFailable(sender, cancelToken);
            } catch (Exception e) {
                deferred.send(Result.<T, E>error((E) e));
            } finally {
                deferred.close();
            }
        }

        public abstract void runMayFailable(Sender<T> sender, CancelToken cancelToken) throws E;

        private static class FailableDeferredSender<T, E extends Exception> implements Sender<T> {
            private Deferred<Result<T, E>> deferred;

            FailableDeferredSender(Deferred<Result<T, E>> deferred) {
                this.deferred = deferred;
            }

            @Override
            public Sender<T> send(T message) {
                deferred.send(Result.<T, E>success(message));
                return this;
            }

            @Override
            public Sender<T> sendAll(Iterable<T> messages) {
                for (T message : messages) deferred.send(Result.<T, E>success(message));
                return this;
            }

            @Override
            public Sender<T> sendAll(T... messages) {
                for (T message : messages) deferred.send(Result.<T, E>success(message));
                return this;
            }
        }
    }

    /**
     * Wraps a {@link me.tatarka.ipromise.Deferred} so that only messages can be sent. The creator
     * then has control over when the {@code Deferred} is closed.
     *
     * @param <T> the message type
     */
    public static interface Sender<T> {
        Sender<T> send(T message);

        Sender<T> sendAll(Iterable<T> messages);

        Sender<T> sendAll(T... messages);
    }
}
