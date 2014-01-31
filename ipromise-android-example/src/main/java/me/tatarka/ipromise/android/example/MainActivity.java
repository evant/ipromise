package me.tatarka.ipromise.android.example;

import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import me.tatarka.ipromise.CancelToken;
import me.tatarka.ipromise.Listener;
import me.tatarka.ipromise.Task;
import me.tatarka.ipromise.Tasks;
import me.tatarka.ipromise.android.PromiseCallback;
import me.tatarka.ipromise.android.PromiseManager;

public class MainActivity extends ActionBarActivity {
    private static final String SLEEP_TASK_INIT = "sleep_task_init";
    private static final String SLEEP_TASK_RESTART = "sleep_task_restart";

    PromiseManager promiseManager;
    ProgressBar progressLaunch;
    Button buttonLaunch;
    ProgressBar progressInit;
    Button buttonInit;
    ProgressBar progressRestart;
    Button buttonRestart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        promiseManager = PromiseManager.get(this);

        setContentView(R.layout.activity_main);

        progressLaunch = (ProgressBar) findViewById(R.id.progress_launch);
        buttonLaunch = (Button) findViewById(R.id.button_launch);
        progressInit = (ProgressBar) findViewById(R.id.progress_init);
        buttonInit = (Button) findViewById(R.id.button_init);
        progressRestart = (ProgressBar) findViewById(R.id.progress_restart);
        buttonRestart = (Button) findViewById(R.id.button_restart);

        // Start a launch
        promiseManager.init(Tasks.of(sleep), new PromiseCallback<String>() {
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
        });

        // Init on button press
        promiseManager.listen(SLEEP_TASK_INIT, new PromiseCallback<String>() {
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
        });

        buttonInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promiseManager.init(SLEEP_TASK_INIT, Tasks.of(sleep));
            }
        });

        // Restart on button press
        promiseManager.listen(SLEEP_TASK_RESTART, new PromiseCallback<String>() {
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
        });

        buttonRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promiseManager.restart(SLEEP_TASK_RESTART, Tasks.of(sleep));
            }
        });
    }

    private static Task.Do<String> sleep = new Task.Do<String>() {
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
}
