package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A progress is a way to return multiple values over time. You cannot construct a progress
 * directly, instead you must get one from a {@link Channel}. Unlike a promise, a progress can only
 * have one listener. This is because as messages are consumed, they are no longer available.
 *
 * @param <T> the type of the message
 */
public class Progress<T> {
    private CancelToken cancelToken;
    private List<T> messageBuffer;
    private Listener<T> listener;
    private boolean hasListener;
    private CloseListener closeListener;
    private boolean hasCloseListener;
    private boolean isClosed;

    /**
     * Constructs a new progress. This is used internally by {@link Channel}.
     */
    Progress() {
        this(new CancelToken());
    }

    /**
     * Constructs a new progress with the given {@link CancelToken}. When the token is canceled, the
     * progress is also canceled. This is used internally by {@link Channel}
     *
     * @param cancelToken the cancel token
     */
    Progress(CancelToken cancelToken) {
        this.cancelToken = cancelToken;
        messageBuffer = new ArrayList<T>();
    }

    /**
     * Constructs a progress which immediately delivers the given messages. This is useful when you
     * can send the messages immediately.
     *
     * @param messages the messages to send
     */
    public Progress(Iterable<T> messages) {
        this.cancelToken = new CancelToken();
        messageBuffer = new ArrayList<T>();
        for (T message : messages) {
            messageBuffer.add(message);
        }
        isClosed = true;
    }

    /**
     * Constructs a progress which immediately delivers the given messages. This is useful when you
     * can send the messages immediately.
     *
     * @param messages the messages to send
     */
    public Progress(T... messages) {
        this(Arrays.asList(messages));
    }

    /**
     * Delivers a message to the progress. This is used internally by {@link Channel}.
     *
     * @param message the message to deliver
     */
    synchronized void deliver(T message) {
        if (cancelToken.isCanceled()) return;

        if (listener != null) {
            listener.receive(message);
        } else {
            messageBuffer.add(message);
        }
    }

    /**
     * Notifies the progress that no more messages will be sent. This is used internally by {@link
     * Channel}.
     */
    synchronized void close() {
        isClosed = true;
        listener = null;

        if (closeListener != null) {
            closeListener.close();
            closeListener = null;
        }
    }

    /**
     * Cancels the {@code Progress}, notifying all listeners and propagating the cancellation to all
     * Progresses that share the {@link CancelToken}. A canceled {@code Progress} will ignore all
     * subsequent messages.
     */
    public synchronized void cancel() {
        cancelToken.cancel();
        messageBuffer.clear();
        listener = null;
        closeListener = null;
    }

    /**
     * Returns if the progress has been canceled.
     *
     * @return true if canceled, false otherwise
     */
    public synchronized boolean isCanceled() {
        return cancelToken.isCanceled();
    }

    /**
     * Listens to a {@code Progress}, receiving messages as they are delivered. Messages are
     * buffered until a {@link Listener} is added, so that none are missed. Only one listener may be
     * added.
     *
     * @param listener the listener to call when the progress receives a message
     * @return the progress for chaining
     * @throws IllegalStateException when a listener has already been added
     */
    public synchronized Progress<T> listen(Listener<T> listener) {
        if (hasListener) {
            throw new AlreadyAddedListenerException(this, listener);
        }

        hasListener = true;

        if (listener != null) {
            for (T message : messageBuffer) {
                listener.receive(message);
            }
        }
        messageBuffer.clear();

        if (!isClosed) {
            this.listener = listener;
        }

        return this;
    }

    /**
     * Listens to a {@code Progress}, receiving a callback when the {@code Progress} is closed, i.e.
     * it wont receive any more messages. If the {@code Progress} is already closed, the callback
     * will be called immediately.
     *
     * @param listener the listener
     * @return the progress for chaining
     */
    public synchronized Progress<T> onClose(CloseListener listener) {
        if (hasCloseListener) {
            throw new AlreadyAddedCloseListenerException(this, listener);
        }

        hasCloseListener = true;

        if (listener != null) {
            if (isClosed) {
                listener.close();
            } else {
                this.closeListener = listener;
            }
        }

        return this;
    }

    /**
     * Constructs a new progress that receives a message when the original progress receives one but
     * passes the result through the given {@link Map} function.
     *
     * @param map  the function to chain the result of the original {@code Progress} to the new
     *             progress
     * @param <T2> the type of the new {@code Progress} message.
     * @return the new {@code Progress}
     */
    public synchronized <T2> Progress<T2> then(final Map<T, T2> map) {
        final Progress<T2> newProgress = new Progress<T2>();
        listen(new Listener<T>() {
            @Override
            public void receive(T message) {
                newProgress.deliver(map.map(message));
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                newProgress.close();
            }
        });
        return newProgress;
    }

    /**
     * Constructs a new {@code Progress} that chains two progresses in succession.
     *
     * @param chain the {@link Chain} that constructs the second {@code Progress}
     * @param <T2>  the type of the second {@code Progress} messages
     * @return the new {@code Progress}
     */
    public synchronized <T2> Progress<T2> then(final Chain<T, Progress<T2>> chain) {
        final Progress<T2> newProgress = new Progress<T2>(cancelToken);
        listen(new Listener<T>() {
            @Override
            public void receive(T message) {
                Progress<T2> chainProgress = chain.chain(message);
                CancelToken.join(cancelToken, chainProgress.cancelToken);
                chainProgress.listen(new Listener<T2>() {
                    @Override
                    public void receive(T2 message) {
                        newProgress.deliver(message);
                    }
                });
            }
        });
        return newProgress;
    }

    public synchronized Progress<T> then(final Filter<T> filter) {
        final Progress<T> newProgress = new Progress<T>(cancelToken);
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
               if (filter.filter(result)) newProgress.deliver(result);
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                newProgress.close();
            }
        });
        return newProgress;
    }

    public synchronized <T2> Progress<T2> then(final T2 start, final Fold<T, T2> fold) {
        final Progress<T2> newProgress =new Progress<T2>(cancelToken);
        final T2[] accumulator = (T2[]) new Object[] { start };
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
                accumulator[0] = fold.fold(accumulator[0], result);
                newProgress.deliver(accumulator[0]);
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                newProgress.close();
            }
        });
        return newProgress;
    }

    public synchronized Progress<T> then(final Fold<T, T> fold) {
        final Progress<T> newProgress = new Progress<T>(cancelToken);
        final T[] accumulator = (T[]) new Object[] { null };
        final boolean[] started = new boolean[] { false };
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
                if (started[0]) {
                    accumulator[0] = fold.fold(accumulator[0], result);
                    newProgress.deliver(accumulator[0]);
                } else {
                    accumulator[0] = result;
                    started[0] = true;
                }
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                newProgress.close();
            }
        });
        return newProgress;
    }

    public synchronized Progress<List<T>> batch(final int size) {
        final Progress<List<T>> newProgress = new Progress<List<T>>(cancelToken);
        final List<T> batchedItems = new ArrayList<T>();
        listen(new Listener<T>() {
            @Override
            public void receive(T result) {
                batchedItems.add(result);
                if (batchedItems.size() >= size) {
                    newProgress.deliver(new ArrayList<T>(batchedItems));
                    batchedItems.clear();
                }
            }
        });
        onClose(new CloseListener() {
            @Override
            public void close() {
                if (!batchedItems.isEmpty()) {
                    newProgress.deliver(new ArrayList<T>(batchedItems));
                    batchedItems.clear();
                }
                newProgress.close();
            }
        });
        return newProgress;
    }

    /**
     * Forces a cast of a progress to a super-type to get around covariance restrictions. {@code T2}
     * must be a superclass of {@code T}. This is safe because you can't directly deliver messages
     * to the cast progress.
     *
     * @param <T2> the type of the message to cast to
     * @return a cast of the progress
     */
    @SuppressWarnings("unchecked")
    public <T2> Progress<T2> cast() {
        return (Progress<T2>) this;
    }

    public static class AlreadyAddedListenerException extends IllegalStateException {
        public AlreadyAddedListenerException(Progress progress, Listener listener) {
            super("Cannot add listener " + listener + " because progress " + progress + " already has a listener");
        }
    }

    public static class AlreadyAddedCloseListenerException extends IllegalStateException {
        public AlreadyAddedCloseListenerException(Progress progress, CloseListener listener) {
            super("Cannot add close listener " + listener + " because progress " + progress + " already has a close listener");
        }
    }
}
