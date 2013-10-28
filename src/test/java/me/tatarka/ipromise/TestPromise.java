package me.tatarka.ipromise;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by evan on 10/26/13
 */
@RunWith(JUnit4.class)
public class TestPromise {
    @Test
    public void testPromiseSuccessBefore() {
        Promise<String, Exception> promise = new Promise<String, Exception>();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.listen(listener);
        promise.deliver(result);

        verify(listener).result(Result.success(result));
    }

    @Test
    public void testPromiseSuccessAfter() {
        Promise<String, Exception> promise = new Promise<String, Exception>();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.deliver(result);
        promise.listen(listener);

        verify(listener).result(Result.success(result));
    }

    @Test
    public void testPromiseErrorBefore() {
        Promise<String, Exception> promise = new Promise<String, Exception>();
        Exception result = new Exception("error");
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.listen(listener);
        promise.deliver(result);

        verify(listener).result(Result.error(result));
    }

    @Test
    public void testPromiseErrorAfter() {
        Promise<String, Exception> promise = new Promise<String, Exception>();
        Exception result = new Exception("error");
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.deliver(result);
        promise.listen(listener);

        verify(listener).result(Result.error(result));
    }

    @Test
    public void testPromiseCancelBefore() {
        Promise<String, Exception> promise = new Promise<String, Exception>();
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.listen(listener);
        promise.cancel();

        verify(listener).result(Result.cancel());
    }

    @Test
    public void testPromiseCancelAfter() {
        Promise<String, Exception> promise = new Promise<String, Exception>();
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.cancel();
        promise.listen(listener);

        verify(listener).result(Result.cancel());
    }

    @Test
    public void testPromiseMapSuccess() {
        Promise<String, Exception> promise = new Promise<String, Exception>();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.then(new Promise.Map<String, Integer>() {
            @Override
            public Integer map(String result) {
                return result.length();
            }
        }).listen(listener);
        promise.deliver(result);

        verify(listener).result(Result.success(result.length()));
    }

    @Test
    public void testPromiseMapError() {
        Promise<String, Exception> promise = new Promise<String, Exception>();
        Exception result = new Exception("error");
        Promise.Listener listener = mock(Promise.Listener.class);
        promise.then(new Promise.Map<String, Integer>() {
            @Override
            public Integer map(String result) {
                return result.length();
            }
        }).listen(listener);
        promise.deliver(result);

        verify(listener).result(Result.error(result));
    }

    @Test
    public void testPromiseChainSuccess() {
        Promise<String, Exception> promise1 = new Promise<String, Exception>();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer, Exception, Exception>() {
            @Override
            public Promise<Integer, Exception> chain(String result) {
                Promise<Integer, Exception> promise2 = new Promise<Integer, Exception>();
                promise2.deliver(result.length());
                return promise2;
            }
        }).listen(listener);
        promise1.deliver(result);

        verify(listener).result(Result.success(result.length()));
    }

    @Test
    public void testPromiseChainError() {
        Promise<String, Exception> promise1 = new Promise<String, Exception>();
        Exception result = new Exception("error");
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer, Exception, Exception>() {
            @Override
            public Promise<Integer, Exception> chain(String result) {
                Promise<Integer, Exception> promise2 = new Promise<Integer, Exception>();
                promise2.deliver(result.length());
                return promise2;
            }
        }).listen(listener);
        promise1.deliver(result);

        verify(listener).result(Result.error(result));
    }

    @Test
    public void testPromiseCancelAbove() {
        Promise<String, Exception> promise1 = new Promise<String, Exception>();
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer, Exception, Exception>() {
            @Override
            public Promise<Integer, Exception> chain(String result) {
                Promise<Integer, Exception> promise2 = new Promise<Integer, Exception>();
                promise2.deliver(result.length());
                return promise2;
            }
        }).listen(listener);
        promise1.cancel();

        verify(listener).result(Result.cancel());
    }

    @Test
    public void testPromiseCancelBelow() {
        Promise<String, Exception> promise1 = new Promise<String, Exception>();
        String result = "success";
        Promise.Listener listener = mock(Promise.Listener.class);
        promise1.then(new Promise.Chain<String, Integer, Exception, Exception>() {
            @Override
            public Promise<Integer, Exception> chain(String result) {
                Promise<Integer, Exception> promise2 = new Promise<Integer, Exception>();
                promise2.deliver(result.length());
                return promise2;
            }
        }).cancel();
        promise1.listen(listener).deliver(result);

        verify(listener).result(Result.cancel());
    }

    @Test
    public void testPromiseChainGenerics() {
        Promise<String, TestException1> promise1 = new Promise<String, TestException1>();
        promise1.then(new Promise.Chain<String, Integer, Exception, TestException2>() {
            @Override
            public Promise<Integer, TestException2> chain(String result) {
                return new Promise<Integer, TestException2>();
            }
        }).listen(new Promise.Adapter<Integer, Exception>() {
            @Override
            public void result(Result<Integer, Exception> result) {
                super.result(result);
            }
        });
    }

    private static class TestException1 extends Exception {

    }

    private static class TestException2 extends Exception {

    }
}
