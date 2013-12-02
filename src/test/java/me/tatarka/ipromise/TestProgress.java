package me.tatarka.ipromise;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class TestProgress {
    @Test
    public void testProgressSendBefore() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2";
        Progress.Listener listener = mock(Progress.Listener.class);
        progress.listen(listener);
        channel.send(result1);
        channel.send(result2);

        verify(listener).receive(result1);
        verify(listener).receive(result2);
    }

    @Test
    public void testProgressSendAfter() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2";
        Progress.Listener listener = mock(Progress.Listener.class);
        channel.send(result1);
        channel.send(result2);
        progress.listen(listener);

        verify(listener).receive(result1);
        verify(listener).receive(result2);
    }

    @Test
    public void testProgressCancelBefore() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        String result = "success";
        Progress.Listener listener = mock(Progress.Listener.class);
        progress.listen(listener);
        progress.cancel();
        channel.send(result);

        verify(listener, never()).receive(result);
        assertThat(progress.isCanceled()).isTrue();
    }

    @Test
    public void testProgressCancelAfter() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        String result = "success";
        Progress.Listener listener = mock(Progress.Listener.class);
        progress.cancel();
        progress.listen(listener);
        channel.send(result);

        verify(listener, never()).receive(result);
        assertThat(progress.isCanceled()).isTrue();
    }

    @Test
    public void testProgressMap() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2--";
        Progress.Listener listener = mock(Progress.Listener.class);
        progress.then(new Map<String, Integer>() {
            @Override
            public Integer map(String result) {
                return result.length();
            }
        }).listen(listener);
        channel.send(result1);
        channel.send(result2);

        verify(listener).receive(result1.length());
        verify(listener).receive(result2.length());
    }

    @Test
    public void testProgressChain() {
        Channel<String> channel1 = new Channel<String>();
        Progress<String> progress1 = channel1.progress();
        String result1 = "result1";
        String result2 = "result2--";
        Progress.Listener listener = mock(Progress.Listener.class);
        progress1.then(new Chain<String, Progress<Integer>>() {
            @Override
            public Progress<Integer> chain(String result) {
                Channel<Integer> channel2 = new Channel<Integer>();
                Progress<Integer> progress2 = channel2.progress();
                channel2.send(result.length());
                channel2.send(result.length() + 1);
                return progress2;
            }
        }).listen(listener);
        channel1.send(result1);
        channel1.send(result2);

        verify(listener).receive(result1.length());
        verify(listener).receive(result1.length() + 1);
        verify(listener).receive(result2.length());
        verify(listener).receive(result2.length() + 1);
    }
}
