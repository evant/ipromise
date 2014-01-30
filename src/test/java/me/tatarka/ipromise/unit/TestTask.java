package me.tatarka.ipromise.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.CancelableTask;
import me.tatarka.ipromise.Chain;
import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Map;
import me.tatarka.ipromise.Pair;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Result;
import me.tatarka.ipromise.Task;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created by evan on 10/26/13
 */
@RunWith(JUnit4.class)
public class TestTask {
    @Test
    public void testTaskRun() {
        final String result = "result";
        Promise<String> promise = Task.run(sameThreadExecutor, new Task.Do<String>() {
            @Override
            public String run(CancelToken cancelToken) {
                return result;
            }
        });
        Listener listener = mock(Listener.class);
        promise.listen(listener);

        verify(listener).receive(result);
    }

    @Test
    public void testTaskRunFailable() {
        final Error error = new Error();
        Promise<Result<String, Error>> promise = Task.run(sameThreadExecutor, new Task.DoFailable<String, Error>() {
            @Override
            public String runFailable(CancelToken cancelToken) throws Error {
                throw error;
            }
        });
        Listener listener = mock(Listener.class);
        promise.listen(listener);

        verify(listener).receive(Result.error(error));
    }

    @Test
    public void testCancelRun() {
        final String result = "result";
        final CancelToken.Listener cancelListener = mock(CancelToken.Listener.class);
        Task<String> task = new Task<String>(sameThreadExecutor, new Task.Do<String>() {
            @Override
            public String run(CancelToken cancelToken) {
                cancelToken.listen(cancelListener);
                return result;
            }
        });
        Promise<String> promise = task.promise();
        promise.cancel();
        task.start();

        verify(cancelListener).canceled();
    }

    private static Executor sameThreadExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    private static class Error extends Exception {

    }
}
