package me.tatarka.ipromise.multithread;

import me.tatarka.ipromise.Deferred;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.umd.cs.mtc.MultiThreadedRunner;
import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.Threaded;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Result;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by evan on 10/26/13
 */
@RunWith(MultiThreadedRunner.class)
public class TestPromiseSuccess extends MultithreadedTestCase {
    static int LISTEN_COUNT = 1000;

    Deferred<String, Exception> deferred = new Deferred<String, Exception>();
    Promise<String, Exception> promise = deferred.promise();
    Promise.Listener listener = mock(Promise.Listener.class);

    @Threaded
    public void thread1() throws InterruptedException {
        Thread.sleep(10);
        deferred.resolve("success");
    }

    @Threaded
    public void thread2() throws InterruptedException {
        for (int i = 0; i < LISTEN_COUNT; i++) {
            promise.listen(listener);
        }
    }

    @Test
    public void test() {
        verify(listener, times(LISTEN_COUNT)).result(Result.success("success"));
    }
}
