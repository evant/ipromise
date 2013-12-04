package me.tatarka.ipromise.performance;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import me.tatarka.ipromise.Channel;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Progress;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestProgressSend {
    @Rule
    public ContiPerfRule i = new ContiPerfRule();

    private Channel<String> channel = new Channel<String>();
    private Progress<String> progress = channel.progress();
    private Channel<String> noListenChannel = new Channel<String>();

    @Before
    public void setup() {
        progress.listen(new Listener<String>() {
            @Override
            public void receive(String result) {
                assertThat(result).isEqualTo("result");
            }
        });
    }

    @Test
    @PerfTest(duration = 500, threads = 20)
    public void testProgressSend() {
        channel.send("result");
    }

    @Test
    @PerfTest(duration = 500, threads = 20)
    public void testNoListenProgressSend() {
        noListenChannel.send("result");
    }
}
