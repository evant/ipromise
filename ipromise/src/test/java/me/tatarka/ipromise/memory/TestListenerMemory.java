package me.tatarka.ipromise.memory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Promise;

@RunWith(JUnit4.class)
public class TestListenerMemory {
    static {
        Promise.setDefaultCallbackExecutor(Promise.getSameThreadCallbackExecutor());
    }

    @Test
    public void testPromiseListenersClearedOnCallback() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        Listener<String> listener = new Listener<String>() {
            @Override
            public void receive(String message) {

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
            public void receive(String message) {

            }
        };
        promise.listen(listener);
        promise.cancel();
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(listener);
        listener = null;

        verifier.assertGarbageCollected("Listener should be collected after cancel");
    }

    @Test
    public void testPromiseListenerClearedOnClose() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        Listener<String> listener = new Listener<String>() {
            @Override
            public void receive(String message) {

            }
        };
        promise.listen(listener);
        deferred.send("result");
        deferred.close();
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(listener);
        listener = null;

        verifier.assertGarbageCollected("Listener should be collected after close");
    }

    @Test
    public void testPromiseListenerClearedOnCloseWithLateListener() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        Listener<String> listener = new Listener<String>() {
            @Override
            public void receive(String message) {

            }
        };
        deferred.send("result");
        deferred.close();
        promise.listen(listener);
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(listener);
        listener = null;

        verifier.assertGarbageCollected("Listener should be collected after close");
    }
}
