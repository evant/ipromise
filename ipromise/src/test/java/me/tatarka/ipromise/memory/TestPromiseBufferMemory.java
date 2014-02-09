package me.tatarka.ipromise.memory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Promise;

@RunWith(JUnit4.class)
public class TestPromiseBufferMemory {
    static {
        Promise.setDefaultCallbackExecutor(Promise.getSameThreadCallbackExecutor());
    }

    @Test
    public void testBufferRetainNone() {
        TestMessage message = new TestMessage();
        Deferred<TestMessage> deferred = new Deferred<TestMessage>(Promise.BUFFER_NONE);
        deferred.send(message);
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(message);
        message = null;
        verifier.assertGarbageCollected("Promise messages should be garbage collected if they are not buffered");
    }

    @Test
    public void testBufferRetainLast() {
        TestMessage message = new TestMessage();
        Deferred<TestMessage> deferred = new Deferred<TestMessage>(Promise.BUFFER_LAST);
        deferred.send(message);
        deferred.send(null); // Make sure message is not last one, as that one is preserved
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(message);
        message = null;
        verifier.assertGarbageCollected("Promise messages should be garbage collected when only the last is buffered");
    }

    private static class TestMessage {

    }
}
