package me.tatarka.ipromise.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import me.tatarka.ipromise.func.Chain;
import me.tatarka.ipromise.func.Map;
import me.tatarka.ipromise.Result;

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

    @Test
    public void testSuccessEquals() {
        Result result1 = Result.success("success");
        Result result2 = Result.success("success");
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    public void testMapSuccess() {
        Result<String, Exception> result1 = Result.success("success");
        Result<Integer, Exception> result2 = result1.onSuccess(new Map<String, Integer>() {
            @Override
            public Integer map(String arg) {
                return arg.length();
            }
        });
        assertThat(result2.getSuccess()).isEqualTo("success".length());
    }

    @Test
    public void testChainSuccess() {
        Result<String, Exception> result1 = Result.success("success");
        Result<Integer, Exception> result2 = result1.onSuccess(new Chain<String, Result<Integer, Exception>>() {
            @Override
            public Result<Integer, Exception> chain(String chain) {
                return Result.success(chain.length());
            }
        });
        assertThat(result2.getSuccess()).isEqualTo("success".length());
    }
}
