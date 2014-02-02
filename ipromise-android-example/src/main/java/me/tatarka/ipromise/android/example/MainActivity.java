package me.tatarka.ipromise.android.example;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import me.tatarka.ipromise.Async;
import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Channel;
import me.tatarka.ipromise.Progress;
import me.tatarka.ipromise.PromiseTask;
import me.tatarka.ipromise.Task;
import me.tatarka.ipromise.Tasks;
import me.tatarka.ipromise.android.AsyncAdapter;
import me.tatarka.ipromise.android.AsyncCallback;
import me.tatarka.ipromise.android.AsyncItem;
import me.tatarka.ipromise.android.AsyncManager;

public class MainActivity extends ActionBarActivity {
    private static final String SLEEP_TASK_INIT = "sleep_task_init";
    private static final String SLEEP_TASK_RESTART = "sleep_task_restart";
    private static final String PROGRESS_TASK = "progress_task";

    AsyncManager asyncManager;
    ProgressBar progressLaunch;
    Button buttonLaunch;
    ProgressBar progressInit;
    Button buttonInit;
    ProgressBar progressRestart;
    Button buttonRestart;
    ProgressBar progressProgress;
    Button buttonProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        asyncManager = AsyncManager.get(this);

        setContentView(R.layout.activity_main);

        progressLaunch = (ProgressBar) findViewById(R.id.progress_launch);
        buttonLaunch = (Button) findViewById(R.id.button_launch);
        progressInit = (ProgressBar) findViewById(R.id.progress_init);
        buttonInit = (Button) findViewById(R.id.button_init);
        progressRestart = (ProgressBar) findViewById(R.id.progress_restart);
        buttonRestart = (Button) findViewById(R.id.button_restart);
        progressProgress = (ProgressBar) findViewById(R.id.progress_progress);
        buttonProgress = (Button) findViewById(R.id.button_progress);

        // Start a launch
        asyncManager.start(
                Tasks.of(sleep),
                new AsyncAdapter<String>() {
                    @Override
                    public void start() {
                        progressLaunch.setVisibility(View.VISIBLE);
                        buttonLaunch.setEnabled(false);
                    }

                    @Override
                    public void receive(String result) {
                        progressLaunch.setVisibility(View.INVISIBLE);
                        buttonLaunch.setEnabled(false);
                        buttonLaunch.setText(result + " (Launch)");
                    }

                    @Override
                    public void save(String result, Bundle outState) {
                        outState.putString(SLEEP_TASK_INIT, result);
                    }

                    @Override
                    public String restore(Bundle savedState) {
                        return savedState.getString(SLEEP_TASK_INIT);
                    }
                }
        );

        // Init on button press
        final AsyncItem<String> initAsync = asyncManager.add(
                SLEEP_TASK_INIT,
                Tasks.of(sleep),
                new AsyncAdapter<String>() {
                    @Override
                    public void start() {
                        progressInit.setVisibility(View.VISIBLE);
                        buttonInit.setEnabled(false);
                    }

                    @Override
                    public void receive(String result) {
                        progressInit.setVisibility(View.INVISIBLE);
                        buttonInit.setEnabled(false);
                        buttonInit.setText(result + " (Init)");
                    }
                }
        );

        buttonInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initAsync.start();
            }
        });

        // Restart on button press
        final AsyncItem<String> restartAsync = asyncManager.add(
                SLEEP_TASK_RESTART,
                Tasks.of(sleep),
                new AsyncAdapter<String>() {
                    @Override
                    public void start() {
                        progressRestart.setVisibility(View.VISIBLE);
                        buttonRestart.setEnabled(false);
                        buttonRestart.setText("Restart on Button Press");
                    }

                    @Override
                    public void receive(String result) {
                        progressRestart.setVisibility(View.INVISIBLE);
                        buttonRestart.setEnabled(true);
                        buttonRestart.setText(result + " (restart)");
                    }
                }
        );

        buttonRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restartAsync.restart();
            }
        });

        // Progress on button press
        final AsyncItem<Integer> progressAsync = asyncManager.add(
                PROGRESS_TASK,
                new Task<Integer>() {
                    @Override
                    public Async<Integer> start() {
                        return countProgress();
                    }
                },
                new AsyncAdapter<Integer>() {
                    @Override
                    public void start() {
                        buttonProgress.setEnabled(false);
                        progressProgress.setProgress(0);
                        progressProgress.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void receive(Integer result) {
                        buttonProgress.setText("Progress running (" + result + ")");
                        progressProgress.setProgress(result);
                    }

                    @Override
                    public void end() {
                        buttonProgress.setText("Restart Progress on Button Press");
                        buttonProgress.setEnabled(true);
                        progressProgress.setVisibility(View.INVISIBLE);
                    }
                }
        );

        buttonProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressAsync.restart();
            }
        });
    }

    private static PromiseTask.Do<String> sleep = new PromiseTask.Do<String>() {
        @Override
        public String run(CancelToken cancelToken) {
            cancelToken.listen(new CancelToken.Listener() {
                @Override
                public void canceled() {
                    Log.d("iPromise LOG", "task canceled");
                }
            });
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Promise completed!";
        }
    };

    private static Progress<Integer> countProgress() {
        final CancelToken cancelToken = new CancelToken();
        final Channel<Integer> channel = new Channel<Integer>(cancelToken);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= 100; i += 10) {
                    if (cancelToken.isCanceled()) {
                        Log.d("iPromise LOG", "task canceled");
                        return;
                    }
                    channel.send(i);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                channel.close();
            }
        }).start();
        return channel.progress();
    }
}
