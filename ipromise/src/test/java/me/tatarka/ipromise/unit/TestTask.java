package me.tatarka.ipromise.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.Executor;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Result;
import me.tatarka.ipromise.task.Task;
import me.tatarka.ipromise.task.Tasks;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by evan on 10/26/13
 */
@RunWith(JUnit4.class)
public class TestTask {
    static {
       Promise.setDefaultCallbackExecutor(Promise.getSameThreadCallbackExecutor());
    }

    @Test
    public void testTaskRun() {
        final String result = "result";
        Promise<String> promise = Tasks.run(sameThreadExecutor, new Task.DoOnce<String>() {
            @Override
            public String runOnce(CancelToken cancelToken) {
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
        Promise<Result<String, Error>> promise = Tasks.run(sameThreadExecutor, new Task.DoOnceFailable<String, Error>() {
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
