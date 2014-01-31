package me.tatarka.ipromise.memory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import me.tatarka.ipromise.Channel;
import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Progress;
import me.tatarka.ipromise.Promise;

@RunWith(JUnit4.class)
public class TestListenerMemory {
    @Test
    public void testPromiseListenersClearedOnCallback() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        Listener<String> listener = new Listener<String>() {
            @Override
            public void receive(String result) {

            }
        };
        promise.listen(listener);
        deferred.resolve("result");
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(listener);
        listener = null;
        verifier.assertGarbageCollected("Listener should be collected after message delivered");
    }

    @Test
    public void testPromiseListenersClearedOnCancel() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        Listener<String> listener = new Listener<String>() {
            @Override
            public void receive(String result) {

            }
        };
        promise.listen(listener);
        promise.cancel();
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(listener);
        listener = null;
        verifier.assertGarbageCollected("Listener should be collected after cancel");
    }

    @Test
    public void testProgressListenerClearedOnCancel() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        Listener<String> listener = new Listener<String>() {
            @Override
            public void receive(String result) {

            }
        };
        progress.listen(listener);
        channel.send("result");
        progress.cancel();
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(listener);
        listener = null;
        verifier.assertGarbageCollected("Listener should be collected after cancel");
    }

    @Test
    public void testProgressListenerClearedOnClose() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        Listener<String> listener = new Listener<String>() {
            @Override
            public void receive(String result) {

            }
        };
        progress.listen(listener);
        channel.send("result");
        channel.close();
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(listener);
        listener = null;
        verifier.assertGarbageCollected("Listener should be collected after close");
    }

    @Test
    public void testProgressListenerClearedOnCloseWithLateListener() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        Listener<String> listener = new Listener<String>() {
            @Override
            public void receive(String result) {

            }
        };
        channel.send("result");
        channel.close();
        progress.listen(listener);
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(listener);
        listener = null;
        verifier.assertGarbageCollected("Listener should be collected after close");
    }
}
