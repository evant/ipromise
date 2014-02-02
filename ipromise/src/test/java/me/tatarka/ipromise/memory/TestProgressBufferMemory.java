package me.tatarka.ipromise.memory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import me.tatarka.ipromise.Channel;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Progress;

@RunWith(JUnit4.class)
public class TestProgressBufferMemory {
    @Test
    public void testBufferRetainNone() {
        TestMessage message = new TestMessage();
        Channel<TestMessage> channel = new Channel<TestMessage>(Progress.RETAIN_NONE);
        channel.send(message);
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(message);
        message = null;
        verifier.assertGarbageCollected("Progress messages should be garbage collected when there is a listener");
    }

    @Test
    public void testBufferRetainLast() {
        TestMessage message = new TestMessage();
        Channel<TestMessage> channel = new Channel<TestMessage>(Progress.RETAIN_LAST);
        channel.send(message);
        channel.send(null); // Make sure message is not last one, as that one is preserved
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(message);
        message = null;
        verifier.assertGarbageCollected("Progress messages should be garbage collected when there is a null listener");
    }

    private static class TestMessage {

    }
}
