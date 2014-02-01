package me.tatarka.ipromise.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.Executor;

import me.tatarka.ipromise.Async;
import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.PromiseExecutorTask;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.PromiseTask;
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
        Promise<String> promise = Tasks.run(sameThreadExecutor, new PromiseTask.Do<String>() {
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
        Promise<Result<String, Error>> promise = Tasks.run(sameThreadExecutor, new PromiseTask.DoFailable<String, Error>() {
            @Override
            public String runFailable(CancelToken cancelToken) throws Error {
                throw error;
            }
        });
        Listener listener = mock(Listener.class);
        promise.listen(listener);

        verify(listener).receive(Result.error(error));
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
