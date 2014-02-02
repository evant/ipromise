package me.tatarka.ipromise;

/**
 * An interface for an object that may be closed. {@link me.tatarka.ipromise.Progress} implements
 * this interface.
 *
 * @author Evan Tatarka
 */
public interface Closeable {
    /**
     * Returns if the object has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();

    /**
     * Registers a listener that will be notified when the object is closed.
     *
     * @param listener the listener
     * @return the {@code Closeable} for chaining
     */
    Closeable onClose(CloseListener listener);
}
