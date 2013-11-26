package me.tatarka.ipromise;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.listen(listener);
        deferred.resolve(result);

        verify(listener).result(result);
    }

    @Test
    public void testPromiseAfter() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        deferred.resolve(result);
        promise.listen(listener);

        verify(listener).result(result);
    }

    @Test
    public void testPromiseCancelBefore() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.listen(listener);
        promise.cancel();
        deferred.resolve(result);

        verify(listener, never()).result(result);
        assertThat(promise.isCanceled()).isTrue();
    }

    @Test
    public void testPromiseCancelAfter() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.cancel();
        promise.listen(listener);
        deferred.resolve(result);

        verify(listener, never()).result(result);
        assertThat(promise.isCanceled()).isTrue();
    }

    @Test
    public void testPromiseMap() {
        Deferred<String> deferred = new Deferred<String>();
        Promise<String> promise = deferred.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.then(new Promise.Map<String, Integer>() {
            @Override
            public Integer map(String result) {
                return result.length();
            }
        }).listen(listener);
        deferred.resolve(result);

        verify(listener).result(result.length());
    }

    @Test
    public void testPromiseChain() {
        Deferred<String> deferred1 = new Deferred<String>();
        Promise<String> promise1 = deferred1.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer>() {
            @Override
            public Promise<Integer> chain(String result) {
                Deferred<Integer> deferred2 = new Deferred<Integer>();
                Promise<Integer> promise2 = deferred2.promise();
                deferred2.resolve(result.length());
                return promise2;
            }
        }).listen(listener);
        deferred1.resolve(result);

        verify(listener).result(result.length());
    }

    @Test
    public void testPromiseCancelAbove() {
        Deferred<String> deferred1 = new Deferred<String>();
        Promise<String> promise1 = deferred1.promise();
        final Deferred<Integer> deferred2 = new Deferred<Integer>();
        final Promise<Integer> promise2 = deferred2.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer>() {
            @Override
            public Promise<Integer> chain(String result) {
                deferred2.resolve(result.length());
                return promise2;
            }
        }).listen(listener);
        promise1.cancel();
        deferred1.resolve(result);

        verify(listener, never()).result(result);
        assertThat(promise1.isCanceled()).isTrue();
    }

    @Test
    public void testPromiseCancelBelow() {
        Deferred<String> deferred1 = new Deferred<String>();
        Promise<String> promise1 = deferred1.promise();
        final Deferred<Integer> deferred2 = new Deferred<Integer>();
        final Promise<Integer> promise2 = deferred2.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer>() {
            @Override
            public Promise<Integer> chain(String result) {
                deferred2.resolve(result.length());
                return promise2;
            }
        }).cancel();
        promise1.listen(listener);
        deferred1.resolve(result);

        verify(listener, never()).result(result);
        assertThat(promise1.isCanceled()).isTrue();
    }
}
