package me.tatarka.ipromise.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Chain;
import me.tatarka.ipromise.Channel;
import me.tatarka.ipromise.CloseListener;
import me.tatarka.ipromise.Filters;
import me.tatarka.ipromise.Folds;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Map;
import me.tatarka.ipromise.Progress;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class TestProgress {
    @Test
    public void testProgressSendBefore() {
        Channel<String> channel = new Channel<String>(Progress.RETAIN_NONE);
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2";
        Listener listener = mock(Listener.class);
        progress.listen(listener);
        channel.send(result1);
        channel.send(result2);

        verify(listener).receive(result1);
        verify(listener).receive(result2);
    }

    @Test
    public void testProgressSendAfterRetainNone() {
        Channel<String> channel = new Channel<String>(Progress.RETAIN_NONE);
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2";
        Listener listener = mock(Listener.class);
        channel.send(result1);
        channel.send(result2);
        progress.listen(listener);

        verify(listener, never()).receive(result1);
        verify(listener, never()).receive(result2);
    }

    @Test
    public void testProgressSendAfterRetainLast() {
        Channel<String> channel = new Channel<String>(Progress.RETAIN_LAST);
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2";
        Listener listener = mock(Listener.class);
        channel.send(result1);
        channel.send(result2);
        progress.listen(listener);

        verify(listener, never()).receive(result1);
        verify(listener).receive(result2);
    }

    @Test
    public void testProgressSendAfterRetainAll() {
        Channel<String> channel = new Channel<String>(Progress.RETAIN_ALL);
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2";
        Listener listener = mock(Listener.class);
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
        Listener listener = mock(Listener.class);
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
        Listener listener = mock(Listener.class);
        progress.cancel();
        progress.listen(listener);
        channel.send(result);

        verify(listener, never()).receive(result);
        assertThat(progress.isCanceled()).isTrue();
    }

    @Test
    public void testProgressMap() {
        Channel<String> channel = new Channel<String>(Progress.RETAIN_NONE);
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2--";
        Listener listener = mock(Listener.class);
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
        Channel<String> channel1 = new Channel<String>(Progress.RETAIN_NONE);
        Progress<String> progress1 = channel1.progress();
        String result1 = "result1";
        String result2 = "result2";
        Listener listener = mock(Listener.class);
        progress1.then(new Chain<String, Progress<String>>() {
            @Override
            public Progress<String> chain(String result) {
                Channel<String> channel2 = new Channel<String>(Progress.RETAIN_ALL);
                Progress<String> progress2 = channel2.progress();
                channel2.send(result + "a");
                channel2.send(result + "b");
                return progress2;
            }
        }).listen(listener);
        channel1.send(result1);
        channel1.send(result2);

        verify(listener).receive(result1 + "a");
        verify(listener).receive(result1 + "b");
        verify(listener).receive(result2 + "a");
        verify(listener).receive(result2 + "b");
    }

    @Test
    public void testDeliverOnNewListenerRetainNone() {
        Channel<String> channel = new Channel<String>(Progress.RETAIN_NONE);
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2";
        Listener listener = mock(Listener.class);
        progress.listen(new Listener<String>() {
            @Override
            public void receive(String result) {

            }
        });
        channel.send(result1);
        channel.send(result2);
        progress.listen(listener);

        verify(listener, never()).receive(result1);
        verify(listener, never()).receive(result2);
    }

    @Test
    public void testDeliverOnNewListenerRetainLast() {
        Channel<String> channel = new Channel<String>(Progress.RETAIN_LAST);
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2";
        Listener listener = mock(Listener.class);
        progress.listen(new Listener<String>() {
            @Override
            public void receive(String result) {

            }
        });
        channel.send(result1);
        channel.send(result2);
        progress.listen(listener);

        verify(listener, never()).receive(result1);
        verify(listener).receive(result2);
    }

    @Test
    public void testDeliverOnNewListenerRetainAll() {
        Channel<String> channel = new Channel<String>(Progress.RETAIN_ALL);
        Progress<String> progress = channel.progress();
        String result1 = "result1";
        String result2 = "result2";
        Listener listener = mock(Listener.class);
        progress.listen(new Listener<String>() {
            @Override
            public void receive(String result) {

            }
        });
        channel.send(result1);
        channel.send(result2);
        progress.listen(listener);

        verify(listener).receive(result1);
        verify(listener).receive(result2);
    }

    @Test(expected = Channel.ClosedChannelException.class)
    public void testCantSendMessageAfterClose() {
        Channel<String> channel = new Channel<String>();
        channel.send("message");
        channel.close();
        channel.send("bad message");
    }

    @Test(expected = Channel.ClosedChannelException.class)
    public void testCantGetProgressAfterClosed() {
        Channel<String> channel = new Channel<String>();
        channel.close();
        channel.progress();
    }

    @Test
    public void testCloseShouldNotPreventListenerFromBeingAdded() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        Listener listener = mock(Listener.class);
        channel.send("message");
        channel.close();
        progress.listen(listener);
        verify(listener).receive("message");
    }

    @Test
    public void testCloseListenerBefore() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        CloseListener listener = mock(CloseListener.class);
        progress.onClose(listener);
        channel.close();
        verify(listener).close();
    }

    @Test
    public void testCloseListenerAfter() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        CloseListener listener = mock(CloseListener.class);
        channel.close();
        progress.onClose(listener);
        verify(listener).close();
    }

    @Test
    public void testFilter() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        Listener listener = mock(Listener.class);
        progress.then(Filters.equal("good")).listen(listener);
        channel.send("bad");
        channel.send("good");
        verify(listener, never()).receive("bad");
        verify(listener).receive("good");
    }

    @Test
    public void testFold() {
        Channel<Integer> channel = new Channel<Integer>();
        Progress<Integer>  progress = channel.progress();
        Listener listener = mock(Listener.class);
        progress.then(0, Folds.sumInt()).listen(listener);
        channel.send(1);
        channel.send(2);
        channel.send(3);
        verify(listener).receive(1);
        verify(listener).receive(3);
        verify(listener).receive(6);
    }

    @Test
    public void testBatch() {
        Channel<String> channel = new Channel<String>();
        Progress<String> progress = channel.progress();
        Listener listener = mock(Listener.class);
        progress.batch(2).listen(listener);
        channel.send("one");
        channel.send("two");
        channel.send("three");
        channel.send("four");
        channel.send("five");
        channel.close();
        verify(listener).receive(Arrays.asList("one", "two"));
        verify(listener).receive(Arrays.asList("three", "four"));
        verify(listener).receive(Arrays.asList("three", "four"));
        verify(listener).receive(Arrays.asList("five"));
    }
}
