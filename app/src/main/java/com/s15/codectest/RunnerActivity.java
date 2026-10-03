package com.s15.codectest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.s15.codectest.measure.MeasurementService;
import com.s15.codectest.measure.RunnerStatus;

// Shows the running session. Owner: runner
public class RunnerActivity extends AppCompatActivity {

    private TextView statusText, trialsText, detailsText;
    private ProgressBar progress;
    private Button abortButton, doneButton;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            updateScreen();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runner);

        statusText = findViewById(R.id.statusText);
        trialsText = findViewById(R.id.trialsText);
        detailsText = findViewById(R.id.detailsText);
        progress = findViewById(R.id.progress);
        abortButton = findViewById(R.id.abortButton);
        doneButton = findViewById(R.id.doneButton);

        abortButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Abort session?")
                .setMessage("Finished trials are kept. The current trial is marked as aborted.")
                .setPositiveButton("Abort", (d, w) -> MeasurementService.stop(this))
                .setNegativeButton("Keep going", null)
                .show());
        doneButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SessionsActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresh);
    }

    private void updateScreen() {
        statusText.setText(RunnerStatus.message);
        trialsText.setText("Trials done: " + RunnerStatus.trialsDone + " / " + RunnerStatus.trialsTotal);

        if (RunnerStatus.trialDurationMs > 0) {
            progress.setProgress((int) (RunnerStatus.trialElapsedMs * 100 / RunnerStatus.trialDurationMs));
        }
        String current = RunnerStatus.currentMa == null ? "-" : String.format("%.1f mA", RunnerStatus.currentMa);
        String temp = RunnerStatus.tempC == null ? "-" : String.format("%.1f °C", RunnerStatus.tempC);
        detailsText.setText("Clip: " + RunnerStatus.clip
                + "\nTime: " + RunnerStatus.trialElapsedMs / 1000 + " / " + RunnerStatus.trialDurationMs / 1000 + " s"
                + "\nCurrent: " + current
                + "\nTemperature: " + temp
                + "\nProblems during trial: " + RunnerStatus.violations);

        abortButton.setVisibility(RunnerStatus.active ? View.VISIBLE : View.GONE);
        doneButton.setVisibility(RunnerStatus.active ? View.GONE : View.VISIBLE);
    }
}
