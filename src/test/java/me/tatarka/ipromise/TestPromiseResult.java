package me.tatarka.ipromise;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class TestPromiseResult {
    @Test
    public void testPromiseSuccess() {
        Deferred<Result<String, Exception>> deferred = new Deferred<Result<String, Exception>>();
        Promise<Result<String, Exception>> promise = new Promise<Result<String, Exception>>();

        promise.then(new Result.Chain<String, Integer, Exception>() {
            @Override
            protected Promise<Result<Integer, Exception>> success(String success) {
                return null;
            }
        });

        Promise.Listener listener = mock(Promise.Listener.class);
        promise.listen(listener);
        deferred.resolve(result);

        verify(listener).result(result);
    }
}
