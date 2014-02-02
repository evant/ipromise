package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A progress is a way to return multiple values over time. You cannot construct a progress
 * directly, instead you must get one from a {@link Channel}.
 *
 * @param <T> the type of the message
 * @author Evan Tatarka
 */
public class Progress<T> implements Async<T>, Closeable {
    /**
     * Don't retain any messages. Messages received before a listener is attached will be dropped.
     */
    public static final int RETAIN_NONE = 0;

    /**
     * Retains only the last message. When a listener is attached, it will immediately receive the
     * last message sent. This is the default.
     */
    public static final int RETAIN_LAST = 1;

    /**
     * Retains all messages. When a listener is attached, it will immediately receive all messages
     * sent up to that point. Be aware of the memory implications of keeping all messages.
     */
    public static final int RETAIN_ALL = 2;

    private int retentionPolicy;
    private CancelToken cancelToken;
    private List<T> messageBuffer;
    private List<Listener<T>> listeners = new ArrayList<Listener<T>>();
    private List<CloseListener> closeListeners = new ArrayList<CloseListener>();
    private boolean isClosed;

    /**
     * Constructs a new {@code Progress}. This is used internally by {@link Channel}.
     */
    Progress() {
        this(RETAIN_LAST, new CancelToken());
    }

    /**
     * Constructs a new {@code Progress} with the given retention policy. This is used internally by
     * {@link Channel}.
     *
     * @param retentionPolicy the retention policy for messages.
     */
    Progress(int retentionPolicy) {
        this(retentionPolicy, new CancelToken());
    }

    /**
     * Constructs a new {@code Progress} with the given {@link CancelToken}. When the token is
     * canceled, the progress is also canceled. This is used internally by {@link Channel}
     *
     * @param cancelToken the cancel token
     */
    Progress(CancelToken cancelToken) {
        this(RETAIN_LAST, cancelToken);
    }

    /**
     * Constructs a new progress with the given retention policy and {@link CancelToken}. When the
     * token is canceled, the progress is also canceled. This is used internally by {@link Channel}
     *
     * @param cancelToken the cancel token
     */
    Progress(int retentionPolicy, CancelToken cancelToken) {
        this.retentionPolicy = retentionPolicy;
        this.cancelToken = cancelToken;

        if (retentionPolicy == RETAIN_LAST) {
            messageBuffer = new ArrayList<T>(1);
        } else if (retentionPolicy == RETAIN_ALL) {
            messageBuffer = new ArrayList<T>();
        }

        cancelToken.listen(new CancelToken.Listener() {
            @Override
            public void canceled() {
                listeners.clear();
                closeListeners.clear();
            }
        });
    }

    /**
     * Constructs a progress which immediately delivers the given messages. This is useful when you
     * can send the messages immediately.
     *
     * @param messages the messages to send
     */
    public Progress(Iterable<T> messages) {
        this.retentionPolicy = RETAIN_ALL;
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
     * The {@link me.tatarka.ipromise.CancelToken} of the {@code Progress}. When the token is
     * canceled, this progress is canceled, and won't receive any more messages.
     *
     * @return the cancel token
     */
    public CancelToken cancelToken() {
        return cancelToken;
    }

    /**
     * Delivers a message to the progress. This is used internally by {@link Channel}.
     *
     * @param message the message to deliver
     */
    synchronized void deliver(T message) {
        if (cancelToken.isCanceled()) return;

        if (retentionPolicy == RETAIN_LAST) {
            if (messageBuffer.isEmpty()) {
                messageBuffer.add(message);
            } else {
                messageBuffer.set(0, message);
            }
        } else if (retentionPolicy == RETAIN_ALL) {
            messageBuffer.add(message);
        }

        for (Listener<T> listener : listeners) {
            listener.receive(message);
        }
    }

    /**
     * Notifies the progress that no more messages will be sent. This is used internally by {@link
     * Channel}.
     */
    synchronized void close() {
        isClosed = true;

        listeners.clear();

        for (CloseListener listener : closeListeners) {
            listener.close();
        }
        closeListeners.clear();
    }

    /**
     * Cancels the {@code Progress}, notifying all listeners and propagating the cancellation to all
     * Progresses that share the {@link CancelToken}. A canceled {@code Progress} will ignore all
     * subsequent messages.
     */
    public synchronized void cancel() {
        cancelToken.cancel();
    }

    /**
     * Returns if the progress has been canceled.
     *
     * @return true if canceled, false otherwise
     */
    public synchronized boolean isCanceled() {
        return cancelToken.isCanceled();
    }

    @Override
    public synchronized boolean isRunning() {
        return !isClosed && !isCanceled();
    }

    /**
     * Returns if the {@code Progress} is closed. A closed progress will not receive any more
     * messages.
     *
     * @return true if closed, false otherwise
     */
    public synchronized boolean isClosed() {
        return isClosed;
    }

    /**
     * Listens to a {@code Progress}, receiving messages as they are delivered. Messages are
     * buffered until a {@link Listener} is added, so that none are missed. If there was already a
     * listener added, the new listener will replace it and receive only the last result given to
     * the {@code Promise}.
     *
     * @param listener the listener to call when the progress receives a message
     * @return the progress for chaining
     */
    @Override
    public synchronized Progress<T> listen(Listener<T> listener) {
        if (listener != null && messageBuffer != null) {
            for (T message : messageBuffer) {
                listener.receive(message);
            }
        }

        if (!isClosed) {
            listeners.add(listener);
        }

        return this;
    }

    /**
     * Listens to a {@code Progress}, receiving a callback when the {@code Progress} is closed, i.e.
     * it wont receive any more messages. If the {@code Progress} is already closed, the callback
     * will be called immediately. If this method is called multiple times, only the latest {@link
     * me.tatarka.ipromise.CloseListener} will be called.
     *
     * @param listener the listener
     * @return the progress for chaining
     */
    public synchronized Progress<T> onClose(CloseListener listener) {
        if (listener != null) {
            if (isClosed) {
                listener.close();
            } else {
                closeListeners.add(listener);
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
        final Progress<T2> newProgress = new Progress<T2>(retentionPolicy, cancelToken);
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

    /**
     * Constructs a new {@code Progress} that filters this {@code Progress}. i.e. the new {@code
     * Progress} will not receive any messages when {@link me.tatarka.ipromise.Filter#filter(Object)}
     * returns false.
     *
     * @param filter the filter
     * @return the new {@code Progress}
     */
    public synchronized Progress<T> then(final Filter<T> filter) {
        final Progress<T> newProgress = new Progress<T>(retentionPolicy, cancelToken);
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

    /**
     * Constructs a new {@code Progress} that the messages of this {@code Progress}.
     *
     * @param size the batch's size. The new {@code Progress} will be called every time this many
     *             messages are sent, and again with the remaining messages when the {@code
     *             Progress} is closed.
     * @return the new {@code Progress}
     */
    public synchronized Progress<List<T>> batch(final int size) {
        final Progress<List<T>> newProgress = new Progress<List<T>>(retentionPolicy, cancelToken);
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
}
