package me.tatarka.ipromise;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by evan on 10/26/13
 */
@RunWith(JUnit4.class)
public class TestPromise {
    @Test
    public void testPromiseSuccessBefore() {
        Deferred<String, Exception> deferred = new Deferred<String, Exception>();
        Promise<String, Exception> promise = deferred.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.listen(listener);
        deferred.resolve(result);

        verify(listener).result(Result.success(result));
    }

    @Test
    public void testPromiseSuccessAfter() {
        Deferred<String, Exception> deferred = new Deferred<String, Exception>();
        Promise<String, Exception> promise = deferred.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        deferred.resolve(result);
        promise.listen(listener);

        verify(listener).result(Result.success(result));
    }

    @Test
    public void testPromiseErrorBefore() {
        Deferred<String, Exception> deferred = new Deferred<String, Exception>();
        Promise<String, Exception> promise = deferred.promise();
        Exception result = new Exception("error");
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.listen(listener);
        deferred.reject(result);

        verify(listener).result(Result.error(result));
    }

    @Test
    public void testPromiseErrorAfter() {
        Deferred<String, Exception> deferred = new Deferred<String, Exception>();
        Promise<String, Exception> promise = deferred.promise();
        Exception result = new Exception("error");
        Promise.Listener listener = mock(Promise.Listener.class);
        deferred.reject(result);
        promise.listen(listener);

        verify(listener).result(Result.error(result));
    }

    @Test
    public void testPromiseCancelBefore() {
        Deferred<String, Exception> deferred = new Deferred<String, Exception>();
        Promise<String, Exception> promise = deferred.promise();
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.listen(listener);
        promise.cancel();

        verify(listener).result(Result.cancel());
    }

    @Test
    public void testPromiseCancelAfter() {
        Deferred<String, Exception> deferred = new Deferred<String, Exception>();
        Promise<String, Exception> promise = deferred.promise();
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.cancel();
        promise.listen(listener);

        verify(listener).result(Result.cancel());
    }

    @Test
    public void testPromiseMapSuccess() {
        Deferred<String, Exception> deferred = new Deferred<String, Exception>();
        Promise<String, Exception> promise = deferred.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.then(new Promise.Map<String, Integer>() {
            @Override
            public Integer map(String result) {
                return result.length();
            }
        }).listen(listener);
        deferred.resolve(result);

        verify(listener).result(Result.success(result.length()));
    }

    @Test
    public void testPromiseMapError() {
        Deferred<String, Exception> deferred = new Deferred<String, Exception>();
        Promise<String, Exception> promise = deferred.promise();
        Exception result = new Exception("error");
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.then(new Promise.Map<String, Integer>() {
            @Override
            public Integer map(String result) {
                return result.length();
            }
        }).listen(listener);
        deferred.reject(result);

        verify(listener).result(Result.error(result));
    }

    @Test
    public void testPromiseChainSuccess() {
        Deferred<String, Exception> deferred1 = new Deferred<String, Exception>();
        Promise<String, Exception> promise1 = deferred1.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer, Exception, Exception>() {
            @Override
            public Promise<Integer, Exception> chain(String result) {
                Deferred<Integer, Exception> deferred2 = new Deferred<Integer, Exception>();
                Promise<Integer, Exception> promise2 = deferred2.promise();
                deferred2.resolve(result.length());
                return promise2;
            }
        }).listen(listener);
        deferred1.resolve(result);

        verify(listener).result(Result.success(result.length()));
    }

    @Test
    public void testPromiseChainError() {
        Deferred<String, Exception> deferred1 = new Deferred<String, Exception>();
        Promise<String, Exception> promise1 = deferred1.promise();
        Exception result = new Exception("error");
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer, Exception, Exception>() {
            @Override
            public Promise<Integer, Exception> chain(String result) {
                Deferred<Integer, Exception> deferred2 = new Deferred<Integer, Exception>();
                Promise<Integer, Exception> promise2 = deferred2.promise();
                deferred2.resolve(result.length());
                return promise2;
            }
        }).listen(listener);
        deferred1.reject(result);

        verify(listener).result(Result.error(result));
    }

    @Test
    public void testPromiseCancelAbove() {
        Deferred<String, Exception> deferred1 = new Deferred<String, Exception>();
        Promise<String, Exception> promise1 = deferred1.promise();
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer, Exception, Exception>() {
            @Override
            public Promise<Integer, Exception> chain(String result) {
                Deferred<Integer, Exception> deferred2 = new Deferred<Integer, Exception>();
                Promise<Integer, Exception> promise2 = deferred2.promise();
                deferred2.resolve(result.length());
                return promise2;
            }
        }).listen(listener);
        promise1.cancel();

        verify(listener).result(Result.cancel());
    }

    @Test
    public void testPromiseCancelBelow() {
        Deferred<String, Exception> deferred1 = new Deferred<String, Exception>();
        Promise<String, Exception> promise1 = deferred1.promise();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer, Exception, Exception>() {
            @Override
            public Promise<Integer, Exception> chain(String result) {
                Deferred<Integer, Exception> deferred2 = new Deferred<Integer, Exception>();
                Promise<Integer, Exception> promise2 = deferred2.promise();
                deferred2.resolve(result.length());
                return promise2;
            }
        }).cancel();
        promise1.listen(listener);
        deferred1.resolve(result);

        verify(listener).result(Result.cancel());
    }
}
