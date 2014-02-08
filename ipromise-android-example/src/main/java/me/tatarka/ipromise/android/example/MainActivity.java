package me.tatarka.ipromise.android.example;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Deferred;
import me.tatarka.ipromise.Promise;
import me.tatarka.ipromise.Task;
import me.tatarka.ipromise.Tasks;
import me.tatarka.ipromise.android.AsyncAdapter;
import me.tatarka.ipromise.android.AsyncItem;
import me.tatarka.ipromise.android.AsyncManager;
import me.tatarka.ipromise.android.SaveCallback;

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
                },
                new SaveCallback<String>() {
                    @Override
                    public void asyncSave(String result, Bundle outState) {
                        outState.putString(SLEEP_TASK_INIT, result);
                    }

                    @Override
                    public String asyncRestore(Bundle savedState) {
                        return savedState.getString(SLEEP_TASK_INIT);
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
                Tasks.of(count),
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

    private static Task.Do<String> sleep = new Task.DoOnce<String>() {
        @Override
        public String runOnce(CancelToken cancelToken) {
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

    private static Task.Do<Integer> count = new Task.Do<Integer>() {
        @Override
        public void run(Deferred<Integer> deferred, CancelToken cancelToken) {
            for (int i = 0; i <= 100; i += 10) {
                if (cancelToken.isCanceled()) {
                    Log.d("iPromise LOG", "task canceled");
                    return;
                }
                deferred.send(i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            deferred.close();
        }
    };
}
