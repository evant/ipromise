package me.tatarka.ipromise;

public class Channel<T> {
    private Progress<T> progress;

    public Channel() {
        progress = new Progress<T>();
    }

    public void send(T message) {
        progress.deliver(message);
    }

    public Progress<T> progress() {
        return progress;
    }
}
