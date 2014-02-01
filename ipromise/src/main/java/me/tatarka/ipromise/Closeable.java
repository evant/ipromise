package me.tatarka.ipromise;

public interface Closeable {
    boolean isClosed();
    Closeable onClose(CloseListener listener);
}
