package com.s15.codectest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.s15.codectest.measure.MeasurementService;
import com.s15.codectest.measure.BatteryMonitor;
import com.s15.codectest.measure.PreflightChecker;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Pick the clips, check the phone is ready, start the session. Owner: setup
public class NewSessionActivity extends AppCompatActivity {

    private static final long CHECK_EVERY_MS = 2000;
    private static final int MIN_CLIPS = 2;

    // One checkbox per clip, kept so we can read which ones are ticked later.
    private final List<CheckBox> clipBoxes = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BatteryMonitor battery;
    private TextView problemsText;
    private CheckBox appsClosedCheck;
    private CheckBox screenOffCheck;
    private Button startButton;
    private List<String> problems = new ArrayList<>();
    private boolean starting = false; // true once start is pressed, so it cannot be pressed twice

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
        appsClosedCheck = findViewById(R.id.appsClosedCheck);
        screenOffCheck = findViewById(R.id.screenOffCheck);
        startButton = findViewById(R.id.startButton);

        addClipBoxes();

        // Re-check the Start button whenever a checklist box changes.
        appsClosedCheck.setOnCheckedChangeListener((box, checked) -> updateStartButton());
        screenOffCheck.setOnCheckedChangeListener((box, checked) -> updateStartButton());

        startButton.setOnClickListener(v -> onStartPressed());
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
            box.setOnCheckedChangeListener((b, checked) -> updateStartButton());
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
        updateStartButton();
    }

    // Task 3: Start is only allowed when the phone is ready, both checklist boxes
    //         are ticked and at least 2 clips are ticked.
    private void updateStartButton() {
        boolean ready = !starting
                && problems.isEmpty()
                && appsClosedCheck.isChecked()
                && screenOffCheck.isChecked()
                && tickedClipFiles().size() >= MIN_CLIPS;
        startButton.setEnabled(ready);
    }

    // Task 4: read the form and start the session.
    private void onStartPressed() {
        starting = true;
        startButton.setEnabled(false);
        handler.removeCallbacks(checkPhone);

        EditText nameInput = findViewById(R.id.nameInput);
        CheckBox pilotCheck = findViewById(R.id.pilotCheck);

        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            name = "Session " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        }

        startSession(name, tickedClipFiles(), pilotCheck.isChecked());
    }

    // The file names of the ticked clips.
    private List<String> tickedClipFiles() {
        List<String> files = new ArrayList<>();
        for (CheckBox box : clipBoxes) {
            if (box.isChecked()) files.add((String) box.getTag());
        }
        return files;
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
