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
    public void testBufferClearedOnListen() {
        TestMessage message = new TestMessage();
        Channel<TestMessage> channel = new Channel<TestMessage>();
        Progress<TestMessage> progress = channel.progress();
        channel.send(message);
        progress.listen(new Listener<TestMessage>() {
            @Override
            public void receive(TestMessage result) {
            }
        });
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(message);
        message = null;
        verifier.assertGarbageCollected("Progress messages should be garbage collected when there is a listener");
    }

    @Test
    public void testBufferClearedOnNullListen() {
        TestMessage message = new TestMessage();
        Channel<TestMessage> channel = new Channel<TestMessage>();
        Progress<TestMessage> progress = channel.progress();
        channel.send(message);
        progress.listen(null);
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(message);
        message = null;
        verifier.assertGarbageCollected("Progress messages should be garbage collected when there is a null listener");
    }

    @Test
    public void testBufferClearedOnCancel() {
        TestMessage message = new TestMessage();
        Channel<TestMessage> channel = new Channel<TestMessage>();
        Progress<TestMessage> progress = channel.progress();
        channel.send(message);
        progress.cancel();
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(message);
        message = null;
        verifier.assertGarbageCollected("Progress messages should be garbage collected when there is a null listener");
    }

    private static class TestMessage {

    }
}
