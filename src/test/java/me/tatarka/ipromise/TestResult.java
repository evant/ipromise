package me.tatarka.ipromise;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

/**
 * Created with IntelliJ IDEA.
 * User: evan
 * Date: 10/26/13
 * Time: 5:36 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(JUnit4.class)
public class TestResult {
    @Test
    public void testSuccess() {
        Result result = Result.success("success");
        assertThat(result.isSuccess());
    }

    @Test
    public void testError() {
        Result result = Result.error(new Exception("error"));
        assertThat(result.isError());
    }

    @Test
    public void testCancel() {
        Result result = Result.cancel();
        assertThat(result.isCanceled());
    }

    @Test
    public void testGetSuccess() throws Exception {
        Result result = Result.success("success");
        assertThat(result.get()).isEqualTo("success");
    }

    @Test(expected = Exception.class)
    public void testGetError() throws Exception {
        Result result = Result.error(new Exception("error"));
        result.get();
        fail("Result should have thrown exception");
    }

    @Test(expected = Result.CanceledException.class)
    public void testCancelError() throws Exception {
        Result result = Result.cancel();
        result.get();
        fail("Result should have thrown cancel exception");
    }

    @Test
    public void testSuccessEquals() {
        Result result1 = Result.success("success");
        Result result2 = Result.success("success");
        assertThat(result1).isEqualTo(result2);
    }
}
