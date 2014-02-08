package me.tatarka.ipromise.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

import me.tatarka.ipromise.func.Chain;
import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.func.Filters;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.func.Map;
import me.tatarka.ipromise.Pair;
import me.tatarka.ipromise.Promise;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created by evan on 10/26/13
 */
@RunWith(JUnit4.class)
public class TestPromise {
    @Test
    public void testPromiseBefore() {
        Deferred<String> deferred = new Deferred<String>(Promise.BUFFER_LAST);
        Promise<String> promise = deferred.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        promise.listen(listener);
        deferred.resolve(result);

        verify(listener).receive(result);
    }

    @Test
    public void testPromiseAfterBufferNone() {
        Deferred<String> deferred = new Deferred<String>(Promise.BUFFER_NONE);
        Promise<String> promise = deferred.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        deferred.resolve(result);
        promise.listen(listener);

        verify(listener, never()).receive(result);
    }

    @Test
    public void testPromiseAfterBufferLast() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        deferred.resolve(result);
        promise.listen(listener);

        verify(listener).receive(result);
    }

    @Test
    public void testPromiseAfterBufferAll() {
        Deferred<String> deferred = new Deferred<String>(Promise.BUFFER_ALL);
        Promise<String> promise = deferred.promise();
        String result1 = "success1";
        String result2 = "success2";
        Listener listener = mock(Listener.class);
        deferred.send(result1).send(result2).close();
        promise.listen(listener);

        verify(listener).receive(result1);
        verify(listener).receive(result2);
    }

    @Test
    public void testPromiseCancelBefore() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        promise.listen(listener);
        promise.cancel();
        deferred.resolve(result);

        verify(listener, never()).receive(result);
        assertThat(promise.isCanceled()).isTrue();
    }

    @Test
    public void testPromiseCancelAfter() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        promise.cancel();
        promise.listen(listener);
        deferred.resolve(result);

        verify(listener, never()).receive(result);
        assertThat(promise.isCanceled()).isTrue();
    }

    @Test
    public void testPromiseMap() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        promise.then(new Map<String, Integer>() {
            @Override
            public Integer map(String result) {
                return result.length();
            }
        }).listen(listener);
        deferred.resolve(result);

        verify(listener).receive(result.length());
    }

    @Test
    public void testPromiseChain() {
        Deferred<String> deferred1 = new Deferred<String>();
        Promise<String> promise1 = deferred1.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        promise1.then(new Chain<String, Promise<Integer>>() {
            @Override
            public Promise<Integer> chain(String result) {
                Deferred<Integer> deferred2 = new Deferred<Integer>();
                Promise<Integer> promise2 = deferred2.promise();
                deferred2.resolve(result.length());
                return promise2;
            }
        }).listen(listener);
        deferred1.resolve(result);

        verify(listener).receive(result.length());
    }

    @Test
    public void testPromiseCancelAbove() {
        Deferred<String> deferred1 = new Deferred<String>();
        Promise<String> promise1 = deferred1.promise();
        final Deferred<Integer> deferred2 = new Deferred<Integer>();
        final Promise<Integer> promise2 = deferred2.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        promise1.then(new Chain<String, Promise<Integer>>() {
            @Override
            public Promise<Integer> chain(String result) {
                deferred2.resolve(result.length());
                return promise2;
            }
        }).listen(listener);
        promise1.cancel();
        deferred1.resolve(result);

        verify(listener, never()).receive(result);
        assertThat(promise1.isCanceled()).isTrue();
    }

    @Test
    public void testPromiseCancelBelow() {
        Deferred<String> deferred1 = new Deferred<String>();
        Promise<String> promise1 = deferred1.promise();
        final Deferred<Integer> deferred2 = new Deferred<Integer>();
        final Promise<Integer> promise2 = deferred2.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        promise1.then(new Chain<String, Promise<Integer>>() {
            @Override
            public Promise<Integer> chain(String result) {
                deferred2.resolve(result.length());
                return promise2;
            }
        }).cancel();
        promise1.listen(listener);
        deferred1.resolve(result);

        verify(listener, never()).receive(result);
        assertThat(promise1.isCanceled()).isTrue();
    }

    @Test
    public void testPromiseAnd() {
        Deferred<String> deferred1 = new Deferred<String>();
        Promise<String> promise1 = deferred1.promise();
        final Deferred<Integer> deferred2 = new Deferred<Integer>();
        final Promise<Integer> promise2 = deferred2.promise();
        String result1 = "success1";
        Integer result2 = 0;
        Listener listener = mock(Listener.class);
        promise1.and(promise2).listen(listener);
        deferred1.resolve(result1);
        deferred2.resolve(result2);

        verify(listener).receive(Pair.of(result1, result2));
    }

    @Test
    public void testPromiseMerge() {
        Deferred<String> deferred1 = new Deferred<String>();
        Promise<String> promise1 = deferred1.promise();
        final Deferred<String> deferred2 = new Deferred<String>();
        final Promise<String> promise2 = deferred2.promise();
        String result1 = "success1";
        String result2 = "success2";
        Listener listener = mock(Listener.class);
        promise1.merge(promise2).listen(listener);
        deferred1.resolve(result1);
        deferred2.resolve(result2);

        verify(listener).receive(result1);
        verify(listener).receive(result2);
    }

    @Test
    public void testFilter() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        Listener listener = mock(Listener.class);
        promise.then(Filters.equal("good")).listen(listener);
        deferred.send("bad");
        deferred.send("good");

        verify(listener, never()).receive("bad");
        verify(listener).receive("good");
    }

    @Test
    public void testBatch() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        Listener listener = mock(Listener.class);
        promise.batch(2).listen(listener);
        deferred.send("one");
        deferred.send("two");
        deferred.send("three");
        deferred.send("four");
        deferred.send("five");
        deferred.close();

        verify(listener).receive(Arrays.asList("one", "two"));
        verify(listener).receive(Arrays.asList("three", "four"));
        verify(listener).receive(Arrays.asList("three", "four"));
        verify(listener).receive(Arrays.asList("five"));
    }
}
