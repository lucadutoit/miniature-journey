package com.s15.codectest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.s15.codectest.measure.MeasurementService;
import com.s15.codectest.measure.BatteryMonitor;
import com.s15.codectest.measure.PreflightChecker;

import java.util.ArrayList;
import java.util.List;

// Pick the clips, check the phone is ready, start the session. Owner: setup
public class NewSessionActivity extends AppCompatActivity {

    private static final long CHECK_EVERY_MS = 2000;

    // One checkbox per clip, kept so we can read which ones are ticked later.
    private final List<CheckBox> clipBoxes = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BatteryMonitor battery;
    private TextView problemsText;
    private List<String> problems = new ArrayList<>();

    // Runs the phone check, then schedules itself again in 2 seconds.
    private final Runnable checkPhone = new Runnable() {
        @Override
        public void run() {
            refreshProblems();
            handler.postDelayed(this, CHECK_EVERY_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_session);

        battery = new BatteryMonitor(this);
        problemsText = findViewById(R.id.problemsText);

        addClipBoxes();
    }

    // Start checking when the screen is visible, stop when it is not (saves battery).
    @Override
    protected void onResume() {
        super.onResume();
        handler.post(checkPhone);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.post(checkPhone);
    }

    // Task 1: one CheckBox per clip, 128k clips and FLAC are ticked default.
    private void addClipBoxes() {
        LinearLayout clipList = findViewById(R.id.clipList);
        for (Clip clip : Clip.ALL) {
            CheckBox box = new CheckBox(this);
            box.setText(clip.label());
            box.setTag(clip.file);
            box.setChecked(clip.kbps == 128 || clip.kbps == 0);
            clipList.addView(box);
            clipBoxes.add(box);
        }
    }

    // Task 2: read the battery, run PreflightChecker and show the result.
    private void refreshProblems() {
        problems = PreflightChecker.problems(battery.snapshot());
        if (problems.isEmpty()) {
            problemsText.setText("Phone is ready.");
            problemsText.setTextColor(0xFF4CAF50); // GREEN
        } else {
            problemsText.setText(String.join("\n", problems));
            problemsText.setTextColor(0xFFFF5252); // RED
        }
    }

    // Creates the session, starts the measurement and opens the running screen.
    private void startSession(String name, List<String> clipFiles, boolean pilot) {
        new Thread(() -> {
            long id = SessionMaker.create(this, name, clipFiles, pilot);
            runOnUiThread(() -> {
                MeasurementService.start(this, id);
                startActivity(new Intent(this, RunnerActivity.class));
                finish();
            });
        }).start();
    }
}
