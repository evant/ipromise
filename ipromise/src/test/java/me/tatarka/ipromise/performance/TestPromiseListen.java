package me.tatarka.ipromise.performance;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Promise;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestPromiseListen {
    @Rule
    public ContiPerfRule i = new ContiPerfRule();

    private Deferred<String> listenDeferred = new Deferred<String>();
    private Promise<String> listenPromise = listenDeferred.promise();

    @Before
    public void setup() {
        listenDeferred.resolve("result");
    }

    @Test
    @PerfTest(duration = 500, threads = 20)
    public void testPromiseListen() {
        listenPromise.listen(new Listener<String>() {
            @Override
            public void receive(String result) {
                assertThat(result).isEqualTo("result");
            }
        });
    }
}
