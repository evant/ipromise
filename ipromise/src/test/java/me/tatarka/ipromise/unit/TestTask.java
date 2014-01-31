package me.tatarka.ipromise.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.Executor;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.PromiseExecutorTask;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Result;
import me.tatarka.ipromise.Task;
import me.tatarka.ipromise.Tasks;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by evan on 10/26/13
 */
@RunWith(JUnit4.class)
public class TestTask {
    @Test
    public void testTaskRun() {
        final String result = "result";
        Promise<String> promise = Tasks.run(sameThreadExecutor, new Task.Do<String>() {
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
        Promise<Result<String, Error>> promise = Tasks.run(sameThreadExecutor, new Task.DoFailable<String, Error>() {
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
        PromiseExecutorTask<String> task = Tasks.of(sameThreadExecutor, new Task.Do<String>() {
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
