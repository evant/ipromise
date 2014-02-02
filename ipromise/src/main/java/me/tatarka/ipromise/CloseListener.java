package me.tatarka.ipromise;

/**
 * A listener that will be notified when the object is closed.
 *
 * @author Evan Tatarka
 * @see me.tatarka.ipromise.Closeable
 */
public interface CloseListener {
    public void close();
}
