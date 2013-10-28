package me.tatarka.ipromise.multithread;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.umd.cs.mtc.MultiThreadedRunner;
import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.Threaded;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Result;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by evan on 10/26/13
 */
@RunWith(MultiThreadedRunner.class)
public class TestPromiseCancel extends MultithreadedTestCase {
    Promise<String, Exception> promise = new Promise<String, Exception>();
    Promise.Listener listener = mock(Promise.Listener.class);

    @Threaded
    public void thread1() {
        waitForTick(1);
        promise.deliver("success");
    }

    @Threaded
    public void thread2() {
        promise.listen(listener);
        promise.cancel();
        waitForTick(1);
    }

    @Test
    public void test() {
        verify(listener).result(Result.cancel());
    }
}
