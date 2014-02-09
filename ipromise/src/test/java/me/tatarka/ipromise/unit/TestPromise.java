package me.tatarka.ipromise.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Pair;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.func.Chain;
import me.tatarka.ipromise.func.Filters;
import me.tatarka.ipromise.func.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by evan on 10/26/13
 */
@RunWith(JUnit4.class)
public class TestPromise {
    static {
        Promise.setDefaultCallbackExecutor(Promise.getSameThreadCallbackExecutor());
    }

    @Test
    public void testPromiseBefore() throws Exception {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        promise.listen(listener);
        deferred.resolve(result);

        verify(listener).receive(result);
    }

    @Test
    public void testPromiseAfterBufferNone() throws Exception {
        Deferred<String> deferred = new Deferred<String>(Promise.BUFFER_NONE);
        Promise<String> promise = deferred.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        deferred.resolve(result);
        promise.listen(listener);

        verify(listener, never()).receive(result);
    }

    @Test
    public void testPromiseAfterBufferLast() throws Exception {
        Deferred<String> deferred = new Deferred<String>(Promise.BUFFER_LAST);
        Promise<String> promise = deferred.promise();
        String result = "success";
        Listener listener = mock(Listener.class);
        deferred.resolve(result);
        promise.listen(listener);

        verify(listener).receive(result);
    }

    @Test
    public void testPromiseAfterBufferAll() throws Exception {
        Deferred<String> deferred = new Deferred<String>(Promise.BUFFER_ALL);
        Promise<String> promise = deferred.promise();
        String result1 = "success1";
        String result2 = "success2";
        Listener listener = mock(Listener.class);
        deferred.resolveAll(result1, result2);
        promise.listen(listener);

        verify(listener).receive(result1);
        verify(listener).receive(result2);
    }

    @Test
    public void testPromiseCancelBefore() throws Exception {
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
    public void testPromiseCancelAfter() throws Exception {
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
    public void testPromiseMap() throws Exception {
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
    public void testPromiseChain() throws Exception {
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
        deferred1.resolve(result);

        verify(listener).receive(result.length());
    }

    @Test
    public void testPromiseCancelAbove() throws Exception {
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
    public void testPromiseCancelBelow() throws Exception {
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
    public void testPromiseAnd() throws Exception {
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
    public void testPromiseMerge() throws Exception {
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
    public void testFilter() throws Exception {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        Listener listener = mock(Listener.class);
        promise.then(Filters.equal("good")).listen(listener);
        deferred.resolveAll("bad", "good");

        verify(listener, never()).receive("bad");
        verify(listener).receive("good");
    }

    @Test
    public void testBatch() throws Exception {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        Listener listener = mock(Listener.class);
        promise.batch(2).listen(listener);
        deferred.resolveAll("one", "two", "three", "four", "five");

        verify(listener).receive(Arrays.asList("one", "two"));
        verify(listener).receive(Arrays.asList("three", "four"));
        verify(listener).receive(Arrays.asList("three", "four"));
        verify(listener).receive(Arrays.asList("five"));
    }
}
